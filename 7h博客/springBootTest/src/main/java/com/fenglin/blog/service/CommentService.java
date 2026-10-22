package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.dto.BlogModels.CommentRequest;
import com.fenglin.blog.dto.BlogModels.CommentVO;
import com.fenglin.blog.entity.Comment;
import com.fenglin.blog.entity.Post;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.CommentMapper;
import com.fenglin.blog.mapper.PostMapper;
import com.fenglin.blog.mapper.UserMapper;
import com.fenglin.blog.security.CurrentUser;
import com.fenglin.blog.security.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章评论。
 *
 * <p>只支持一层回复：{@code parentId} 指向父评论，回复对象即父评论作者。
 * 删除父评论时，子评论不会被连带删除，而是摘成顶层评论。
 */
@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public CommentService(CommentMapper commentMapper, PostMapper postMapper,
                          UserMapper userMapper, NotificationService notificationService) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    public List<CommentVO> list(Long postId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .orderByAsc(Comment::getCreatedAt)
                .orderByAsc(Comment::getId));

        // 一次性取回相关用户，避免 N+1
        Map<Long, User> users = new HashMap<>();
        for (Comment comment : comments) {
            users.computeIfAbsent(comment.getUserId(), userMapper::selectById);
        }

        List<CommentVO> list = new ArrayList<>(comments.size());
        for (Comment comment : comments) {
            User author = users.get(comment.getUserId());
            Long replyToUserId = null;
            String replyToName = null;
            if (comment.getParentId() != null) {
                Comment parent = commentMapper.selectById(comment.getParentId());
                if (parent != null) {
                    replyToUserId = parent.getUserId();
                    User parentAuthor = users.computeIfAbsent(parent.getUserId(), userMapper::selectById);
                    replyToName = parentAuthor == null ? null : parentAuthor.getNickname();
                }
            }
            list.add(new CommentVO(
                    comment.getId(),
                    comment.getPostId(),
                    comment.getUserId(),
                    author == null ? null : author.getNickname(),
                    author == null ? null : author.getAvatar(),
                    author == null ? null : author.getFaction(),
                    comment.getParentId(),
                    replyToUserId,
                    replyToName,
                    comment.getContent(),
                    comment.getCreatedAt()));
        }
        return list;
    }

    @Transactional
    public CommentVO create(Long postId, CommentRequest request) {
        CurrentUser current = CurrentUserHolder.require();
        Post post = postMapper.selectById(postId);
        // 只要不是草稿就能评论，自定义状态同样允许
        if (post == null || "DRAFT".equalsIgnoreCase(post.getStatus())) {
            throw BizException.notFound("文章不存在或仍是草稿");
        }

        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentMapper.selectById(request.parentId());
            if (parent == null || !parent.getPostId().equals(postId)) {
                throw BizException.badRequest("回复的评论不存在");
            }
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(current.userId());
        comment.setParentId(parent == null ? null : parent.getId());
        comment.setContent(request.content().trim());
        comment.setStatus("NORMAL");
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        String preview = comment.getContent().length() > 100
                ? comment.getContent().substring(0, 100) : comment.getContent();
        if (parent != null) {
            notificationService.notify(parent.getUserId(), current.userId(),
                    NotificationService.TYPE_REPLY, postId, comment.getId(), preview);
        }
        notificationService.notify(post.getAuthorId(), current.userId(),
                NotificationService.TYPE_COMMENT, postId, comment.getId(), preview);

        User author = userMapper.selectById(current.userId());
        return new CommentVO(
                comment.getId(),
                postId,
                current.userId(),
                author == null ? null : author.getNickname(),
                author == null ? null : author.getAvatar(),
                author == null ? null : author.getFaction(),
                comment.getParentId(),
                parent == null ? null : parent.getUserId(),
                parent == null ? null : nickNameOf(parent.getUserId()),
                comment.getContent(),
                comment.getCreatedAt());
    }

    @Transactional
    public void delete(Long commentId) {
        CurrentUser current = CurrentUserHolder.require();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BizException.notFound("评论不存在或已删除");
        }
        if (!comment.getUserId().equals(current.userId()) && !current.isAdmin()) {
            throw BizException.forbidden("只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);

        // 子评论摘成顶层，避免整棵回复树消失。
        // 这里必须用 UpdateWrapper 显式 set null —— 用实体对象更新时，
        // MyBatis-Plus 默认 NOT_NULL 策略会跳过 null 字段，生成没有 SET 子句的非法 SQL。
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .set(Comment::getParentId, null)
                .eq(Comment::getParentId, commentId));
    }

    private String nickNameOf(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getNickname();
    }
}
