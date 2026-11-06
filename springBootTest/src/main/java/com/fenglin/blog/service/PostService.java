package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.common.PageResult;
import com.fenglin.blog.dto.BlogModels.PostRequest;
import com.fenglin.blog.dto.BlogModels.PostVO;
import com.fenglin.blog.dto.BlogModels.TagVO;
import com.fenglin.blog.entity.Category;
import com.fenglin.blog.entity.Comment;
import com.fenglin.blog.entity.Post;
import com.fenglin.blog.entity.PostLike;
import com.fenglin.blog.entity.PostTag;
import com.fenglin.blog.entity.Tag;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.CategoryMapper;
import com.fenglin.blog.mapper.CommentMapper;
import com.fenglin.blog.mapper.PostLikeMapper;
import com.fenglin.blog.mapper.PostMapper;
import com.fenglin.blog.mapper.PostTagMapper;
import com.fenglin.blog.mapper.TagMapper;
import com.fenglin.blog.mapper.UserMapper;
import com.fenglin.blog.security.CurrentUser;
import com.fenglin.blog.security.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 文章：列表、详情、新建、编辑、删除、置顶 */
@Service
public class PostService {

    /** 草稿：唯一被特殊对待的状态（对外不可见）。其余状态一律对外可见。 */
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    /** 标签名长度上限，与 blog_tag.name varchar(50) 对齐 */
    private static final int TAG_NAME_MAX = 50;

    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;
    private final CommentMapper commentMapper;
    private final PostLikeMapper postLikeMapper;
    private final UserMapper userMapper;
    private final LikeService likeService;
    private final LevelService levelService;

    public PostService(PostMapper postMapper, CategoryMapper categoryMapper, TagMapper tagMapper,
                       PostTagMapper postTagMapper, CommentMapper commentMapper,
                       PostLikeMapper postLikeMapper, UserMapper userMapper, LikeService likeService,
                       LevelService levelService) {
        this.postMapper = postMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.postTagMapper = postTagMapper;
        this.commentMapper = commentMapper;
        this.postLikeMapper = postLikeMapper;
        this.userMapper = userMapper;
        this.likeService = likeService;
        this.levelService = levelService;
    }

    /* ================= 读 ================= */

    /**
     * 已发布文章分页：置顶优先。
     * sort = latest（默认）按发布时间倒序；sort = hot 按热度加权（浏览 + 点赞×5 + 回复×3）。
     */
    public PageResult<PostVO> listPublished(long current, long size, String keyword,
                                            Long categoryId, Long tagId, String sort) {
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>()
                .ne(Post::getStatus, DRAFT);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            query.and(wrapper -> wrapper.like(Post::getTitle, kw).or().like(Post::getSummary, kw));
        }
        if (categoryId != null) {
            query.eq(Post::getCategoryId, categoryId);
        }
        if (tagId != null) {
            List<Long> postIds = postIdsOfTag(tagId);
            if (postIds.isEmpty()) {
                return new PageResult<>(0, current, size, 0, Collections.emptyList());
            }
            query.in(Post::getId, postIds);
        }
        if ("hot".equalsIgnoreCase(sort)) {
            // 固定字符串拼接（无用户输入）。回复数不在本表，用子查询统计未删除评论
            query.last("ORDER BY pinned DESC, (view_count + like_count * 5 + "
                    + "(SELECT COUNT(*) FROM blog_comment cc WHERE cc.post_id = blog_post.id AND cc.deleted = 0) * 3"
                    + ") DESC, id DESC");
        } else {
            query.orderByDesc(Post::getPinned)
                    .orderByDesc(Post::getPinnedAt)
                    .orderByDesc(Post::getPublishedAt)
                    .orderByDesc(Post::getId);
        }
        return toPageResult(postMapper.selectPage(pageOf(current, size), query));
    }

    /** 各吧（分类）的帖子数：社区广场吧导航用 */
    public List<Map<String, Object>> categoryStats() {
        List<Map<String, Object>> rows = postMapper.selectMaps(
                new QueryWrapper<Post>()
                        .select("category_id", "COUNT(*) AS cnt")
                        .ne("status", DRAFT)
                        .isNotNull("category_id")
                        .groupBy("category_id"));
        Map<Long, Long> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object cid = row.get("category_id");
            Object cnt = row.get("cnt");
            if (cid != null && cnt != null) {
                try {
                    counts.put(Long.parseLong(cid.toString()), Long.parseLong(cnt.toString()));
                } catch (NumberFormatException ignored) {
                    // 脏数据直接跳过
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category c : categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder))) {
            // 用 HashMap 而不是 Map.of：吧主可能为空（系统内置分类），Map.of 不接受 null 值
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            item.put("slug", c.getSlug());
            item.put("count", counts.getOrDefault(c.getId(), 0L));
            item.put("ownerId", c.getOwnerId());
            User owner = c.getOwnerId() == null ? null : userMapper.selectById(c.getOwnerId());
            item.put("ownerName", owner == null ? null
                    : (owner.getNickname() != null ? owner.getNickname() : owner.getUsername()));
            result.add(item);
        }
        return result;
    }

    /** 文章详情并累加浏览量 */
    @Transactional
    public PostVO getPublishedBySlug(String slug) {
        Post post = postMapper.selectOne(new LambdaQueryWrapper<Post>()
                .eq(Post::getSlug, slug)
                .ne(Post::getStatus, DRAFT));
        if (post == null) {
            throw BizException.notFound("文章不存在或仍是草稿");
        }
        postMapper.incrementViewCount(post.getId());
        post.setViewCount((post.getViewCount() == null ? 0L : post.getViewCount()) + 1);
        return toVO(post);
    }

    /**
     * 当前用户的文章（含草稿）。
     *
     * @param status 可选筛选：DRAFT 只看草稿；PUBLISHED（或 VISIBLE）只看非草稿；
     *               空 / ALL 则不过滤。状态本身不限制取值，这里只对草稿作区分。
     */
    public PageResult<PostVO> listMine(long current, long size, String status) {
        Long authorId = CurrentUserHolder.require().userId();
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>()
                .eq(Post::getAuthorId, authorId);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            String s = status.trim();
            if (DRAFT.equalsIgnoreCase(s)) {
                query.eq(Post::getStatus, DRAFT);
            } else {
                query.ne(Post::getStatus, DRAFT);
            }
        }
        query.orderByDesc(Post::getPinned)
                .orderByDesc(Post::getCreatedAt);
        return toPageResult(postMapper.selectPage(pageOf(current, size), query));
    }

    /** 某用户已发布文章 */
    public PageResult<PostVO> listByAuthor(Long authorId, long current, long size) {
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>()
                .eq(Post::getAuthorId, authorId)
                .ne(Post::getStatus, DRAFT)
                .orderByDesc(Post::getPinned)
                .orderByDesc(Post::getPublishedAt)
                .orderByDesc(Post::getId);
        return toPageResult(postMapper.selectPage(pageOf(current, size), query));
    }

    /**
     * 管理端：全站文章（含草稿），由 {@code AuthInterceptor} 的 @RequireAdmin 保证权限。
     *
     * @param keyword 按标题模糊搜索，可空
     * @param status  DRAFT / PUBLISHED 精确筛选；空 / ALL 则不过滤
     */
    public PageResult<PostVO> listAll(long current, long size, String keyword, String status) {
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>();
        if (keyword != null && !keyword.isBlank()) {
            query.like(Post::getTitle, keyword.trim());
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            query.eq(Post::getStatus, status.trim().toUpperCase());
        }
        query.orderByDesc(Post::getCreatedAt);
        return toPageResult(postMapper.selectPage(pageOf(current, size), query));
    }

    /* ================= 写 ================= */

    @Transactional
    public PostVO create(PostRequest request) {
        Long authorId = CurrentUserHolder.require().userId();
        Category category = categoryMapper.selectById(request.categoryId());
        if (category == null) {
            throw BizException.badRequest("所选分类不存在");
        }

        String status = normalizeStatus(request.status());
        LocalDateTime now = LocalDateTime.now();
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setCategoryId(category.getId());
        post.setTitle(request.title().trim());
        post.setSlug(buildSlug(request.slug(), request.title(), null));
        post.setSummary(request.summary() == null ? "" : request.summary().trim());
        post.setContent(request.content());
        post.setCoverUrl(request.coverUrl() == null ? "" : request.coverUrl().trim());
        post.setStatus(status);
        post.setViewCount(0L);
        post.setLikeCount(0L);
        post.setPinned(Boolean.TRUE.equals(request.pinned()) ? 1 : 0);
        post.setPinnedAt(Boolean.TRUE.equals(request.pinned()) ? now : null);
        post.setPublishedAt(DRAFT.equalsIgnoreCase(status) ? null : now);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        postMapper.insert(post);

        replaceTags(post.getId(), request.tagIds(), request.tags());
        // 发布（非草稿）才计入经验，草稿不重复刷经验
        if (!DRAFT.equalsIgnoreCase(status)) {
            levelService.addExp(authorId, LevelService.EXP_POST);
        }
        return toVO(post);
    }

    @Transactional
    public PostVO update(Long postId, PostRequest request) {
        Post post = requireOwnPost(postId);
        boolean wasDraft = DRAFT.equalsIgnoreCase(post.getStatus());
        Category category = categoryMapper.selectById(request.categoryId());
        if (category == null) {
            throw BizException.badRequest("所选分类不存在");
        }

        String status = normalizeStatus(request.status());
        LocalDateTime now = LocalDateTime.now();
        post.setCategoryId(category.getId());
        post.setTitle(request.title().trim());
        post.setSummary(request.summary() == null ? "" : request.summary().trim());
        post.setContent(request.content());
        post.setCoverUrl(request.coverUrl() == null ? "" : request.coverUrl().trim());
        if (!post.getSlug().equals(request.slug())) {
            post.setSlug(buildSlug(request.slug(), request.title(), postId));
        }
        // 从草稿切到非草稿时补记发布时间
        if (!DRAFT.equalsIgnoreCase(status) && post.getPublishedAt() == null) {
            post.setPublishedAt(now);
        }
        post.setStatus(status);
        post.setUpdatedAt(now);
        postMapper.updateById(post);

        replaceTags(post.getId(), request.tagIds(), request.tags());
        // 草稿转为发布时补发一次经验
        if (wasDraft && !DRAFT.equalsIgnoreCase(status)) {
            levelService.addExp(post.getAuthorId(), LevelService.EXP_POST);
        }
        return toVO(post);
    }

    @Transactional
    public void delete(Long postId) {
        Post post = requireOwnPost(postId);
        postMapper.deleteById(post.getId());
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, post.getId()));
    }

    @Transactional
    public PostVO togglePin(Long postId, boolean pinned) {
        Post post = requireOwnPost(postId);
        LocalDateTime now = LocalDateTime.now();
        post.setPinned(pinned ? 1 : 0);
        post.setPinnedAt(pinned ? now : null);
        post.setUpdatedAt(now);
        postMapper.updateById(post);
        return toVO(post);
    }

    /* ================= 辅助 ================= */

    /** 取出文章并校验归属：作者本人、本吧吧主或管理员 */
    private Post requireOwnPost(Long postId) {
        CurrentUser current = CurrentUserHolder.require();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BizException.notFound("文章不存在或已删除");
        }
        if (!post.getAuthorId().equals(current.userId()) && !current.isAdmin() && !isBaOwnerOf(post, current)) {
            throw BizException.forbidden("只能操作自己的文章，或由本吧吧主管理");
        }
        return post;
    }

    /** 当前用户是否是该文章所属吧的吧主 */
    private boolean isBaOwnerOf(Post post, CurrentUser current) {
        if (post.getCategoryId() == null) {
            return false;
        }
        Category category = categoryMapper.selectById(post.getCategoryId());
        return category != null && category.getOwnerId() != null
                && category.getOwnerId().equals(current.userId());
    }

    private Page<Post> pageOf(long current, long size) {
        return new Page<>(Math.max(1, current), Math.max(1, Math.min(size, 100)));
    }

    private PageResult<PostVO> toPageResult(IPage<Post> page) {
        List<PostVO> records = new ArrayList<>(page.getRecords().size());
        for (Post post : page.getRecords()) {
            records.add(toVO(post));
        }
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), records);
    }

    private PostVO toVO(Post post) {
        User author = userMapper.selectById(post.getAuthorId());
        Category category = post.getCategoryId() == null
                ? null : categoryMapper.selectById(post.getCategoryId());
        long commentCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, post.getId())
                .and(w -> w.isNull(Comment::getStatus)
                        .or().ne(Comment::getStatus, CommentService.STATUS_HIDDEN)));
        long likeCount = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, post.getId()));
        return new PostVO(
                post.getId(),
                post.getAuthorId(),
                author == null ? null : author.getNickname(),
                author == null ? null : author.getAvatar(),
                author == null ? null : author.getFaction(),
                post.getCategoryId(),
                category == null ? null : category.getName(),
                post.getTitle(),
                post.getSlug(),
                post.getSummary(),
                post.getContent(),
                post.getCoverUrl(),
                post.getStatus(),
                post.getViewCount(),
                likeCount == 0 ? (post.getLikeCount() == null ? 0L : post.getLikeCount()) : likeCount,
                commentCount,
                likeService.likedByCurrentUser(post.getId()),
                post.getPinned() != null && post.getPinned() == 1,
                post.getPinnedAt(),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                tagsOf(post.getId()));
    }

    private List<String> tagsOf(Long postId) {
        List<PostTag> relations = postTagMapper.selectList(
                new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId)
                        .orderByAsc(PostTag::getId));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = new ArrayList<>(relations.size());
        for (PostTag relation : relations) {
            tagIds.add(relation.getTagId());
        }
        Map<Long, String> nameById = new HashMap<>();
        for (Tag tag : tagMapper.selectBatchIds(tagIds)) {
            nameById.put(tag.getId(), tag.getName());
        }
        List<String> names = new ArrayList<>(relations.size());
        for (PostTag relation : relations) {
            String name = nameById.get(relation.getTagId());
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * 全量替换文章标签，先清后插。
     * 两个入参都为 null 时视为"这次请求没管标签"，直接返回，避免把已有标签抹掉；
     * 传空数组则按"用户清空了标签"处理。
     */
    private void replaceTags(Long postId, List<Long> tagIds, List<String> tagNames) {
        if (tagIds == null && tagNames == null) {
            return;
        }
        Set<Long> unique = new HashSet<>();
        if (tagIds != null) {
            for (Long tagId : tagIds) {
                if (tagId != null && tagMapper.selectById(tagId) != null) {
                    unique.add(tagId);
                }
            }
        }
        unique.addAll(resolveTagIds(tagNames));

        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId));
        if (unique.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long tagId : unique) {
            PostTag relation = new PostTag();
            relation.setPostId(postId);
            relation.setTagId(tagId);
            relation.setCreatedAt(now);
            postTagMapper.insert(relation);
        }
    }

    /** 标签名 → 标签 id：先按名查找（忽略首尾空格），库里没有就顺手建一个 */
    private List<Long> resolveTagIds(List<String> tagNames) {
        List<Long> ids = new ArrayList<>();
        if (tagNames == null || tagNames.isEmpty()) {
            return ids;
        }
        LocalDateTime now = LocalDateTime.now();
        for (String raw : tagNames) {
            if (raw == null) {
                continue;
            }
            String name = raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            if (name.length() > TAG_NAME_MAX) {
                name = name.substring(0, TAG_NAME_MAX);
            }
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getName, name)
                    .last("limit 1"));
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tag.setSlug(uniqueTagSlug(name));
                tag.setCreatedAt(now);
                tagMapper.insert(tag);
            }
            ids.add(tag.getId());
        }
        return ids;
    }

    /** 标签 slug 全局唯一；中文等无法转写的名字退化成时间戳形式 */
    private String uniqueTagSlug(String name) {
        String raw = slugify(name);
        if (raw.isEmpty()) {
            raw = "tag-" + System.currentTimeMillis();
        }
        String base = raw.length() > 60 ? raw.substring(0, 60) : raw;
        String candidate = base;
        int suffix = 2;
        while (tagMapper.selectCount(new LambdaQueryWrapper<Tag>().eq(Tag::getSlug, candidate)) > 0) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private List<Long> postIdsOfTag(Long tagId) {
        List<PostTag> relations = postTagMapper.selectList(
                new LambdaQueryWrapper<PostTag>().eq(PostTag::getTagId, tagId));
        List<Long> ids = new ArrayList<>(relations.size());
        for (PostTag relation : relations) {
            ids.add(relation.getPostId());
        }
        return ids;
    }

    /** slug 缺失时由标题生成一个 ASCII 安全的短标识；中文标题会退化成时间戳形式 */
    private String buildSlug(String slug, String title, Long excludeId) {
        String raw = slug == null || slug.isBlank() ? slugify(title) : slugify(slug);
        if (raw.isEmpty()) {
            raw = "post-" + System.currentTimeMillis();
        }
        String candidate = raw;
        int suffix = 2;
        while (slugExists(candidate, excludeId)) {
            candidate = raw + "-" + suffix++;
        }
        return candidate;
    }

    private boolean slugExists(String slug, Long excludeId) {
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>().eq(Post::getSlug, slug);
        if (excludeId != null) {
            query.ne(Post::getId, excludeId);
        }
        return postMapper.selectCount(query) > 0;
    }

    private static String slugify(String input) {
        if (input == null) {
            return "";
        }
        String lowered = input.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lowered.length());
        for (int i = 0; i < lowered.length(); i++) {
            char c = lowered.charAt(i);
            boolean keep = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (keep) {
                sb.append(c);
            } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') {
                sb.append('-');
            }
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 状态归一化：不限制取值。
     * 只有 DRAFT（忽略大小写）被当作草稿，其余原样保留，空值回退 PUBLISHED。
     */
    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return PUBLISHED;
        }
        String trimmed = status.trim();
        return DRAFT.equalsIgnoreCase(trimmed) ? DRAFT : trimmed;
    }
}
