package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 文章：列表、详情、新建、编辑、删除、置顶 */
@Service
public class PostService {

    /** 草稿：唯一被特殊对待的状态（对外不可见）。其余状态一律对外可见。 */
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";

    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;
    private final CommentMapper commentMapper;
    private final PostLikeMapper postLikeMapper;
    private final UserMapper userMapper;
    private final LikeService likeService;

    public PostService(PostMapper postMapper, CategoryMapper categoryMapper, TagMapper tagMapper,
                       PostTagMapper postTagMapper, CommentMapper commentMapper,
                       PostLikeMapper postLikeMapper, UserMapper userMapper, LikeService likeService) {
        this.postMapper = postMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.postTagMapper = postTagMapper;
        this.commentMapper = commentMapper;
        this.postLikeMapper = postLikeMapper;
        this.userMapper = userMapper;
        this.likeService = likeService;
    }

    /* ================= 读 ================= */

    /** 已发布文章分页：置顶优先，其余按发布时间倒序 */
    public PageResult<PostVO> listPublished(long current, long size, String keyword,
                                            Long categoryId, Long tagId) {
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
        query.orderByDesc(Post::getPinned)
                .orderByDesc(Post::getPinnedAt)
                .orderByDesc(Post::getPublishedAt)
                .orderByDesc(Post::getId);
        return toPageResult(postMapper.selectPage(pageOf(current, size), query));
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

        replaceTags(post.getId(), request.tagIds());
        return toVO(post);
    }

    @Transactional
    public PostVO update(Long postId, PostRequest request) {
        Post post = requireOwnPost(postId);
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

        replaceTags(post.getId(), request.tagIds());
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

    /** 取出文章并校验归属：作者本人或管理员 */
    private Post requireOwnPost(Long postId) {
        CurrentUser current = CurrentUserHolder.require();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BizException.notFound("文章不存在或已删除");
        }
        if (!post.getAuthorId().equals(current.userId()) && !current.isAdmin()) {
            throw BizException.forbidden("只能操作自己的文章");
        }
        return post;
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
                .eq(Comment::getPostId, post.getId()));
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

    private List<TagVO> tagsOf(Long postId) {
        List<PostTag> relations = postTagMapper.selectList(
                new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = new ArrayList<>(relations.size());
        for (PostTag relation : relations) {
            tagIds.add(relation.getTagId());
        }
        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        List<TagVO> list = new ArrayList<>(tags.size());
        for (Tag tag : tags) {
            list.add(new TagVO(tag.getId(), tag.getName(), tag.getSlug()));
        }
        return list;
    }

    /** 全量替换文章标签，先清后插 */
    private void replaceTags(Long postId, List<Long> tagIds) {
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        Set<Long> unique = new HashSet<>(tagIds);
        LocalDateTime now = LocalDateTime.now();
        for (Long tagId : unique) {
            if (tagId == null || tagMapper.selectById(tagId) == null) {
                continue;
            }
            PostTag relation = new PostTag();
            relation.setPostId(postId);
            relation.setTagId(tagId);
            relation.setCreatedAt(now);
            postTagMapper.insert(relation);
        }
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
