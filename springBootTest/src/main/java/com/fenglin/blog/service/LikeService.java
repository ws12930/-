package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.entity.Post;
import com.fenglin.blog.entity.PostLike;
import com.fenglin.blog.mapper.PostLikeMapper;
import com.fenglin.blog.mapper.PostMapper;
import com.fenglin.blog.security.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 文章点赞 / 取消点赞 */
@Service
public class LikeService {

    private final PostLikeMapper postLikeMapper;
    private final PostMapper postMapper;
    private final NotificationService notificationService;

    public LikeService(PostLikeMapper postLikeMapper, PostMapper postMapper,
                       NotificationService notificationService) {
        this.postLikeMapper = postLikeMapper;
        this.postMapper = postMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public LikeResult toggle(Long postId) {
        Long userId = CurrentUserHolder.require().userId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BizException.notFound("文章不存在或已删除");
        }

        PostLike existing = postLikeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));

        boolean liked;
        if (existing == null) {
            PostLike record = new PostLike();
            record.setPostId(postId);
            record.setUserId(userId);
            record.setCreatedAt(LocalDateTime.now());
            postLikeMapper.insert(record);
            liked = true;
        } else {
            postLikeMapper.deleteById(existing.getId());
            liked = false;
        }

        // 用真实关联表行数校正冗余计数，避免并发下计数漂移
        long likeCount = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId));
        post.setLikeCount(likeCount);
        postMapper.updateById(post);

        if (liked) {
            notificationService.notify(post.getAuthorId(), userId, NotificationService.TYPE_LIKE,
                    postId, null, null);
        } else {
            notificationService.revoke(post.getAuthorId(), userId, NotificationService.TYPE_LIKE, postId);
        }

        return new LikeResult(postId, likeCount, liked);
    }

    /** 当前用户是否已点赞（供列表页填充 liked 字段） */
    public boolean likedByCurrentUser(Long postId) {
        var current = CurrentUserHolder.get();
        if (current == null) {
            return false;
        }
        return postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, current.userId())) > 0;
    }

    public record LikeResult(Long postId, long likeCount, boolean liked) {
    }
}
