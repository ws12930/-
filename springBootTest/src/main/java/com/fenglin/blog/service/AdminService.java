package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.common.PageResult;
import com.fenglin.blog.dto.AdminModels.AdminCategoryVO;
import com.fenglin.blog.dto.AdminModels.AdminCheckinVO;
import com.fenglin.blog.dto.AdminModels.AdminCommentVO;
import com.fenglin.blog.dto.AdminModels.AdminMessageVO;
import com.fenglin.blog.dto.AdminModels.AdminUserVO;
import com.fenglin.blog.dto.AdminModels.CheckinSummary;
import com.fenglin.blog.dto.AdminModels.MessageSummary;
import com.fenglin.blog.entity.Category;
import com.fenglin.blog.entity.Checkin;
import com.fenglin.blog.entity.Comment;
import com.fenglin.blog.entity.Message;
import com.fenglin.blog.entity.Post;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.CategoryMapper;
import com.fenglin.blog.mapper.CheckinMapper;
import com.fenglin.blog.mapper.CommentMapper;
import com.fenglin.blog.mapper.MessageMapper;
import com.fenglin.blog.mapper.PostMapper;
import com.fenglin.blog.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 管理端：用户（含管理员权限升降级）、评论审核、签到记录、私信、社区（吧）。
 * 权限由 {@code AuthInterceptor} 的 @RequireAdmin 保证。
 */
@Service
public class AdminService {

    /** 评论被隐藏后前台不再展示，但数据保留，可随时恢复 */
    public static final String COMMENT_NORMAL = "NORMAL";
    public static final String COMMENT_HIDDEN = "HIDDEN";

    /** 用户角色：普通用户 / 管理员 */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    /** 吧名长度与前台创建保持一致（2-20 字） */
    private static final int CATEGORY_NAME_MIN = 2;
    private static final int CATEGORY_NAME_MAX = 20;
    private static final int CATEGORY_DESC_MAX = 200;

    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final CheckinMapper checkinMapper;
    private final MessageMapper messageMapper;
    private final CategoryMapper categoryMapper;
    private final LevelService levelService;

    public AdminService(UserMapper userMapper, CommentMapper commentMapper, PostMapper postMapper,
                        CheckinMapper checkinMapper, MessageMapper messageMapper,
                        CategoryMapper categoryMapper, LevelService levelService) {
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.checkinMapper = checkinMapper;
        this.messageMapper = messageMapper;
        this.categoryMapper = categoryMapper;
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

    /**
     * 升级 / 取消管理员权限。
     *
     * <p>只允许 ADMIN / USER 两种角色；为了避免把自己锁在后台外面，
     * 任何情况下都不允许修改自己的角色。
     */
    @Transactional
    public void setUserRole(long userId, String role, long operatorId) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!ROLE_ADMIN.equals(normalized) && !ROLE_USER.equals(normalized)) {
            throw BizException.badRequest("角色只能是 ADMIN（管理员）或 USER（普通用户）");
        }
        if (Objects.equals(userId, operatorId)) {
            throw BizException.badRequest("不能修改自己的角色，请让其他管理员操作");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (normalized.equalsIgnoreCase(user.getRole())) {
            return;
        }
        user.setRole(normalized);
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

    /* ==================== 私信管理 ==================== */

    /**
     * 全站私信列表。
     *
     * @param keyword 匹配消息正文，或匹配发送者 / 接收者的昵称、用户名
     */
    public PageResult<AdminMessageVO> listMessages(long current, long size, String keyword) {
        LambdaQueryWrapper<Message> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            List<Long> matchedUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getNickname, kw)
                            .or()
                            .like(User::getUsername, kw))
                    .stream().map(User::getId).toList();
            query.and(w -> {
                w.like(Message::getContent, kw);
                if (!matchedUserIds.isEmpty()) {
                    w.or().in(Message::getSenderId, matchedUserIds)
                            .or().in(Message::getReceiverId, matchedUserIds);
                }
            });
        }
        query.orderByDesc(Message::getCreatedAt).orderByDesc(Message::getId);

        IPage<Message> page = messageMapper.selectPage(
                new Page<>(Math.max(1, current), Math.max(1, Math.min(size, 100))), query);

        // 一次性把本页涉及的双方资料查回来，避免逐条查询
        Set<Long> userIds = new HashSet<>();
        for (Message message : page.getRecords()) {
            if (message.getSenderId() != null) {
                userIds.add(message.getSenderId());
            }
            if (message.getReceiverId() != null) {
                userIds.add(message.getReceiverId());
            }
        }
        Map<Long, User> users = new HashMap<>();
        for (Long id : userIds) {
            users.put(id, userMapper.selectById(id));
        }

        List<AdminMessageVO> records = new ArrayList<>(page.getRecords().size());
        for (Message message : page.getRecords()) {
            boolean recalled = message.getRecalled() != null && message.getRecalled() == 1;
            records.add(new AdminMessageVO(
                    message.getId(),
                    message.getSenderId(),
                    displayName(users.get(message.getSenderId())),
                    message.getReceiverId(),
                    displayName(users.get(message.getReceiverId())),
                    recalled ? "" : message.getContent(),
                    message.getReadFlag(),
                    recalled ? 1 : 0,
                    message.getCreatedAt()));
        }
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), records);
    }

    /** 私信概览：总条数 / 今日新增 / 未读条数（撤回的不算未读） */
    public MessageSummary messageSummary() {
        long total = messageMapper.selectCount(new LambdaQueryWrapper<>());
        long today = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .ge(Message::getCreatedAt, LocalDate.now().atStartOfDay()));
        long unread = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getReadFlag, 0)
                .ne(Message::getRecalled, 1));
        return new MessageSummary(total, today, unread);
    }

    /** 管理员删除私信：物理删除，并把引用它的消息摘掉引用，避免悬空 quote_id */
    @Transactional
    public void deleteMessage(long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw BizException.notFound("私信不存在或已删除");
        }
        messageMapper.deleteById(messageId);
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .set(Message::getQuoteId, null)
                .eq(Message::getQuoteId, messageId));
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        return user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername() : user.getNickname();
    }

    /* ==================== 社区（吧）管理 ==================== */

    /** 全站吧列表：含吧主、帖子数与启用状态 */
    public PageResult<AdminCategoryVO> listCategories(long current, long size, String keyword) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            query.and(w -> w.like(Category::getName, kw).or().like(Category::getDescription, kw));
        }
        query.orderByAsc(Category::getSortOrder).orderByAsc(Category::getId);

        IPage<Category> page = categoryMapper.selectPage(
                new Page<>(Math.max(1, current), Math.max(1, Math.min(size, 100))), query);

        Map<Long, Long> postCounts = new HashMap<>();
        List<Long> ids = page.getRecords().stream().map(Category::getId).toList();
        if (!ids.isEmpty()) {
            // 只统计本页，避免每次翻页都扫全表；草稿不计入
            List<Map<String, Object>> rows = postMapper.selectMaps(new QueryWrapper<Post>()
                    .select("category_id", "COUNT(*) AS cnt")
                    .ne("status", "DRAFT")
                    .in("category_id", ids)
                    .groupBy("category_id"));
            for (Map<String, Object> row : rows) {
                Object cid = row.get("category_id");
                Object cnt = row.get("cnt");
                if (cid != null && cnt != null) {
                    try {
                        postCounts.put(Long.parseLong(cid.toString()), Long.parseLong(cnt.toString()));
                    } catch (NumberFormatException ignored) {
                        // 脏数据跳过
                    }
                }
            }
        }

        Map<Long, User> owners = new HashMap<>();
        List<AdminCategoryVO> records = new ArrayList<>(page.getRecords().size());
        for (Category category : page.getRecords()) {
            User owner = category.getOwnerId() == null ? null
                    : owners.computeIfAbsent(category.getOwnerId(), userMapper::selectById);
            records.add(new AdminCategoryVO(
                    category.getId(),
                    category.getName(),
                    category.getSlug(),
                    category.getDescription(),
                    category.getOwnerId(),
                    displayName(owner),
                    postCounts.getOrDefault(category.getId(), 0L),
                    category.getStatus(),
                    category.getSortOrder(),
                    category.getCreatedAt()));
        }
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), records);
    }

    /** 启用 / 停用吧：停用后前台社区广场不再展示 */
    @Transactional
    public void setCategoryStatus(long categoryId, int status) {
        if (status != 0 && status != 1) {
            throw BizException.badRequest("状态只能是 0（停用）或 1（启用）");
        }
        Category category = requireCategory(categoryId);
        category.setStatus(status);
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    /** 管理员改吧：与前台不同，管理员可以改名 */
    @Transactional
    public void updateCategory(long categoryId, String name, String description) {
        Category category = requireCategory(categoryId);
        String trimmedName = name == null ? "" : name.trim();
        if (!trimmedName.isEmpty() && !trimmedName.equals(category.getName())) {
            if (trimmedName.length() < CATEGORY_NAME_MIN || trimmedName.length() > CATEGORY_NAME_MAX) {
                throw BizException.badRequest("吧名需要 " + CATEGORY_NAME_MIN + "-" + CATEGORY_NAME_MAX + " 个字");
            }
            Long same = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                    .eq(Category::getName, trimmedName)
                    .ne(Category::getId, categoryId));
            if (same != null && same > 0) {
                throw BizException.badRequest("同名的吧已经存在了");
            }
            category.setName(trimmedName);
        }
        String desc = description == null ? "" : description.trim();
        if (desc.length() > CATEGORY_DESC_MAX) {
            throw BizException.badRequest("简介最多 " + CATEGORY_DESC_MAX + " 个字");
        }
        category.setDescription(desc);
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    /** 删除吧：吧下还有帖子时拒绝，避免帖子变成无主的孤儿数据 */
    @Transactional
    public void deleteCategory(long categoryId) {
        requireCategory(categoryId);
        Long posts = postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getCategoryId, categoryId));
        if (posts != null && posts > 0) {
            throw BizException.badRequest("这个吧下还有 " + posts + " 篇帖子，请先转移或删除后再删吧");
        }
        categoryMapper.deleteById(categoryId);
    }

    private Category requireCategory(long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BizException.notFound("这个吧不存在或已被删除");
        }
        return category;
    }
}
