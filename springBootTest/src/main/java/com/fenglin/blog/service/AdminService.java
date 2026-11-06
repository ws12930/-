package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.common.PageResult;
import com.fenglin.blog.dto.AdminModels.AdminCheckinVO;
import com.fenglin.blog.dto.AdminModels.AdminCommentVO;
import com.fenglin.blog.dto.AdminModels.AdminUserVO;
import com.fenglin.blog.dto.AdminModels.CheckinSummary;
import com.fenglin.blog.entity.Checkin;
import com.fenglin.blog.entity.Comment;
import com.fenglin.blog.entity.Post;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.CheckinMapper;
import com.fenglin.blog.mapper.CommentMapper;
import com.fenglin.blog.mapper.PostMapper;
import com.fenglin.blog.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 管理端：用户、评论审核、签到记录。权限由 {@code AuthInterceptor} 的 @RequireAdmin 保证。
 */
@Service
public class AdminService {

    /** 评论被隐藏后前台不再展示，但数据保留，可随时恢复 */
    public static final String COMMENT_NORMAL = "NORMAL";
    public static final String COMMENT_HIDDEN = "HIDDEN";

    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final CheckinMapper checkinMapper;
    private final LevelService levelService;

    public AdminService(UserMapper userMapper, CommentMapper commentMapper, PostMapper postMapper,
                        CheckinMapper checkinMapper, LevelService levelService) {
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.checkinMapper = checkinMapper;
        this.levelService = levelService;
    }

    /** 用户列表：支持昵称 / 用户名模糊搜索 */
    public PageResult<AdminUserVO> listUsers(long current, long size, String keyword) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            query.and(w -> w.like(User::getNickname, kw).or().like(User::getUsername, kw));
        }
        query.orderByDesc(User::getCreatedAt).orderByDesc(User::getId);
        IPage<User> page = userMapper.selectPage(
                new Page<>(Math.max(1, current), Math.max(1, Math.min(size, 100))), query);
        List<AdminUserVO> records = new ArrayList<>(page.getRecords().size());
        for (User user : page.getRecords()) {
            int exp = levelService.expOf(user);
            records.add(new AdminUserVO(
                    user.getId(),
                    user.getUsername(),
                    user.getNickname(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus(),
                    exp,
                    levelService.levelOf(exp),
                    levelService.titleOf(exp),
                    user.getCreatedAt()));
        }
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), records);
    }

    /** 启用 / 禁用账号：1 正常，0 禁用；不允许禁用自己 */
    @Transactional
    public void setUserStatus(long userId, int status, long adminId) {
        if (status != 0 && status != 1) {
            throw BizException.badRequest("状态只能是 0（禁用）或 1（正常）");
        }
        if (Objects.equals(userId, adminId) && status == 0) {
            throw BizException.badRequest("不能禁用自己的账号");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /* ==================== 评论审核 ==================== */

    /**
     * 评论列表。
     *
     * @param status ALL / NORMAL（正常）/ HIDDEN（已隐藏），空视为 ALL
     * @param keyword 内容关键词
     */
    public PageResult<AdminCommentVO> listComments(long current, long size, String keyword, String status) {
        LambdaQueryWrapper<Comment> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            query.like(Comment::getContent, keyword.trim());
        }
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (COMMENT_HIDDEN.equals(normalized)) {
            query.eq(Comment::getStatus, COMMENT_HIDDEN);
        } else if (COMMENT_NORMAL.equals(normalized)) {
            query.and(w -> w.isNull(Comment::getStatus).or().ne(Comment::getStatus, COMMENT_HIDDEN));
        } else if (!normalized.isEmpty() && !"ALL".equals(normalized)) {
            throw BizException.badRequest("status 只支持 ALL / NORMAL / HIDDEN");
        }
        query.orderByDesc(Comment::getCreatedAt).orderByDesc(Comment::getId);

        IPage<Comment> page = commentMapper.selectPage(
                new Page<>(Math.max(1, current), Math.max(1, Math.min(size, 100))), query);

        // 批量取作者与文章，避免 N+1
        Map<Long, User> users = new HashMap<>();
        Map<Long, Post> posts = new HashMap<>();
        for (Comment comment : page.getRecords()) {
            users.computeIfAbsent(comment.getUserId(), userMapper::selectById);
            posts.computeIfAbsent(comment.getPostId(), postMapper::selectById);
        }

        List<AdminCommentVO> records = new ArrayList<>(page.getRecords().size());
        for (Comment comment : page.getRecords()) {
            User author = users.get(comment.getUserId());
            Post post = posts.get(comment.getPostId());
            boolean hidden = COMMENT_HIDDEN.equalsIgnoreCase(comment.getStatus());
            records.add(new AdminCommentVO(
                    comment.getId(),
                    comment.getPostId(),
                    post == null ? null : post.getTitle(),
                    post == null ? null : post.getSlug(),
                    comment.getUserId(),
                    author == null ? null : (author.getNickname() == null
                            ? author.getUsername() : author.getNickname()),
                    comment.getContent(),
                    hidden ? COMMENT_HIDDEN : COMMENT_NORMAL,
                    hidden,
                    comment.getCreatedAt()));
        }
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), records);
    }

    /** 审核：NORMAL 恢复展示 / HIDDEN 隐藏下架 */
    @Transactional
    public void setCommentStatus(long commentId, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!COMMENT_NORMAL.equals(normalized) && !COMMENT_HIDDEN.equals(normalized)) {
            throw BizException.badRequest("状态只能是 NORMAL（正常）或 HIDDEN（隐藏）");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BizException.notFound("评论不存在或已删除");
        }
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .set(Comment::getStatus, normalized)
                .set(Comment::getUpdatedAt, LocalDateTime.now())
                .eq(Comment::getId, commentId));
    }

    /** 管理员删除评论：子回复摘成顶层，与用户端删除行为保持一致 */
    @Transactional
    public void deleteComment(long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BizException.notFound("评论不存在或已删除");
        }
        commentMapper.deleteById(commentId);
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .set(Comment::getParentId, null)
                .eq(Comment::getParentId, commentId));
    }

    /* ==================== 签到记录 ==================== */

    public PageResult<AdminCheckinVO> listCheckins(long current, long size, String keyword) {
        LambdaQueryWrapper<Checkin> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getNickname, kw)
                            .or()
                            .like(User::getUsername, kw))
                    .stream().map(User::getId).toList();
            if (userIds.isEmpty()) {
                return new PageResult<>(0, Math.max(1, current), Math.max(1, size), 0, List.of());
            }
            query.in(Checkin::getUserId, userIds);
        }
        query.orderByDesc(Checkin::getCheckinDate).orderByDesc(Checkin::getId);

        IPage<Checkin> page = checkinMapper.selectPage(
                new Page<>(Math.max(1, current), Math.max(1, Math.min(size, 100))), query);

        Map<Long, User> users = new HashMap<>();
        List<AdminCheckinVO> records = new ArrayList<>(page.getRecords().size());
        for (Checkin checkin : page.getRecords()) {
            User user = users.computeIfAbsent(checkin.getUserId(), userMapper::selectById);
            records.add(new AdminCheckinVO(
                    checkin.getId(),
                    checkin.getUserId(),
                    user == null ? null : user.getUsername(),
                    user == null ? null : user.getNickname(),
                    checkin.getCheckinDate(),
                    checkin.getStreak(),
                    checkin.getExpGained(),
                    checkin.getCreatedAt()));
        }
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), records);
    }

    /** 签到概览：今日签到人数、今日签到次数、累计次数、最长连续天数 */
    public CheckinSummary checkinSummary() {
        LocalDate today = LocalDate.now();
        long todayCount = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getCheckinDate, today));
        long total = checkinMapper.selectCount(new LambdaQueryWrapper<>());
        Checkin best = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .orderByDesc(Checkin::getStreak)
                .last("LIMIT 1"));
        int maxStreak = best == null || best.getStreak() == null ? 0 : best.getStreak();
        // (user_id, checkin_date) 唯一，今日次数即今日人数
        return new CheckinSummary(todayCount, todayCount, total, maxStreak);
    }
}
