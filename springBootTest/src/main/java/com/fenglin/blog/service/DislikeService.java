package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.entity.BaDislike;
import com.fenglin.blog.entity.Category;
import com.fenglin.blog.entity.Post;
import com.fenglin.blog.entity.PostDislike;
import com.fenglin.blog.mapper.BaDislikeMapper;
import com.fenglin.blog.mapper.CategoryMapper;
import com.fenglin.blog.mapper.PostDislikeMapper;
import com.fenglin.blog.mapper.PostMapper;
import com.fenglin.blog.security.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「不感兴趣」：帖子 / 吧两级，按用户维度记录，只影响本人信息流。
 *
 * <p>增删都做成幂等：重复标记不会插入重复行，重复撤销也不会报错，
 * 这样前端的「标记 → toast 撤销」与「屏蔽管理 → 恢复」可共用同一组接口。
 */
@Service
public class DislikeService {

    private final PostDislikeMapper postDislikeMapper;
    private final BaDislikeMapper baDislikeMapper;
    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;

    public DislikeService(PostDislikeMapper postDislikeMapper, BaDislikeMapper baDislikeMapper,
                          PostMapper postMapper, CategoryMapper categoryMapper) {
        this.postDislikeMapper = postDislikeMapper;
        this.baDislikeMapper = baDislikeMapper;
        this.postMapper = postMapper;
        this.categoryMapper = categoryMapper;
    }

    /* ==================== 写：标记 / 撤销 ==================== */

    @Transactional
    public void markPost(Long postId) {
        Long userId = CurrentUserHolder.require().userId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BizException.notFound("帖子不存在或已删除");
        }
        if (existsPostDislike(userId, postId)) {
            return;
        }
        PostDislike record = new PostDislike();
        record.setUserId(userId);
        record.setPostId(postId);
        record.setCreatedAt(LocalDateTime.now());
        postDislikeMapper.insert(record);
    }

    @Transactional
    public void unmarkPost(Long postId) {
        Long userId = CurrentUserHolder.require().userId();
        postDislikeMapper.delete(new LambdaQueryWrapper<PostDislike>()
                .eq(PostDislike::getUserId, userId)
                .eq(PostDislike::getPostId, postId));
    }

    @Transactional
    public void markBa(Long categoryId) {
        Long userId = CurrentUserHolder.require().userId();
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BizException.notFound("这个吧不存在");
        }
        if (existsBaDislike(userId, categoryId)) {
            return;
        }
        BaDislike record = new BaDislike();
        record.setUserId(userId);
        record.setCategoryId(categoryId);
        record.setCreatedAt(LocalDateTime.now());
        baDislikeMapper.insert(record);
    }

    @Transactional
    public void unmarkBa(Long categoryId) {
        Long userId = CurrentUserHolder.require().userId();
        baDislikeMapper.delete(new LambdaQueryWrapper<BaDislike>()
                .eq(BaDislike::getUserId, userId)
                .eq(BaDislike::getCategoryId, categoryId));
    }

    /* ==================== 读：过滤用 id 集合 ==================== */

    /** 当前用户不感兴趣的帖子 id；游客返回空列表 */
    public List<Long> dislikedPostIds() {
        var current = CurrentUserHolder.get();
        if (current == null) {
            return List.of();
        }
        return selectPostIds(current.userId());
    }

    /** 当前用户不感兴趣的吧 id；游客返回空列表 */
    public List<Long> dislikedBaIds() {
        var current = CurrentUserHolder.get();
        if (current == null) {
            return List.of();
        }
        return selectBaIds(current.userId());
    }

    /** 当前用户已屏蔽的帖子 / 吧明细，供「屏蔽管理」弹窗恢复用 */
    public Map<String, Object> listMine() {
        Long userId = CurrentUserHolder.require().userId();
        List<Long> postIds = selectPostIds(userId);
        List<Long> baIds = selectBaIds(userId);

        List<Map<String, Object>> posts = new ArrayList<>();
        for (Post post : postIds.isEmpty() ? List.<Post>of() : postMapper.selectBatchIds(postIds)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", post.getId());
            item.put("title", post.getTitle());
            posts.add(item);
        }

        List<Map<String, Object>> bas = new ArrayList<>();
        for (Category category : baIds.isEmpty()
                ? List.<Category>of() : categoryMapper.selectBatchIds(baIds)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", category.getId());
            item.put("name", category.getName());
            bas.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("postIds", postIds);
        result.put("baIds", baIds);
        result.put("posts", posts);
        result.put("bas", bas);
        return result;
    }

    private List<Long> selectPostIds(Long userId) {
        return postDislikeMapper.selectList(new LambdaQueryWrapper<PostDislike>()
                        .eq(PostDislike::getUserId, userId)
                        .orderByDesc(PostDislike::getCreatedAt))
                .stream().map(PostDislike::getPostId).toList();
    }

    private List<Long> selectBaIds(Long userId) {
        return baDislikeMapper.selectList(new LambdaQueryWrapper<BaDislike>()
                        .eq(BaDislike::getUserId, userId)
                        .orderByDesc(BaDislike::getCreatedAt))
                .stream().map(BaDislike::getCategoryId).toList();
    }

    private boolean existsPostDislike(Long userId, Long postId) {
        return postDislikeMapper.selectCount(new LambdaQueryWrapper<PostDislike>()
                .eq(PostDislike::getUserId, userId)
                .eq(PostDislike::getPostId, postId)) > 0;
    }

    private boolean existsBaDislike(Long userId, Long categoryId) {
        return baDislikeMapper.selectCount(new LambdaQueryWrapper<BaDislike>()
                .eq(BaDislike::getUserId, userId)
                .eq(BaDislike::getCategoryId, categoryId)) > 0;
    }
}
