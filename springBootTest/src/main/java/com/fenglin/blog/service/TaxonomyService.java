package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.dto.BlogModels.CategoryVO;
import com.fenglin.blog.dto.BlogModels.TagVO;
import com.fenglin.blog.entity.Category;
import com.fenglin.blog.entity.Tag;
import com.fenglin.blog.mapper.CategoryMapper;
import com.fenglin.blog.mapper.TagMapper;
import com.fenglin.blog.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 分类与标签 */
@Service
public class TaxonomyService {

    private static final int NAME_MIN = 2;
    private static final int NAME_MAX = 20;
    private static final int DESC_MAX = 200;
    /** 每人最多创建的吧数，与前端提示一致 */
    private static final int MAX_BA_PER_USER = 3;

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public TaxonomyService(CategoryMapper categoryMapper, TagMapper tagMapper) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    /** 启用状态优先，其次按 sort_order、id */
    public List<CategoryVO> listCategories() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        List<CategoryVO> list = new ArrayList<>(categories.size());
        for (Category category : categories) {
            list.add(new CategoryVO(category.getId(), category.getName(),
                    category.getSlug(), category.getDescription()));
        }
        return list;
    }

    public List<TagVO> listTags() {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
        List<TagVO> list = new ArrayList<>(tags.size());
        for (Tag tag : tags) {
            list.add(new TagVO(tag.getId(), tag.getName(), tag.getSlug()));
        }
        return list;
    }

    /* ==================== 吧（用户自建分类） ==================== */

    /** 建吧：创建者即吧主 */
    @Transactional
    public CategoryVO createCategory(String name, String description, long userId) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw BizException.badRequest("请输入吧名");
        }
        // 前端对话框写的是「吧名，2-20 个字」，这里按同一规则校验
        if (trimmed.length() < NAME_MIN || trimmed.length() > NAME_MAX) {
            throw BizException.badRequest("吧名需要 " + NAME_MIN + "-" + NAME_MAX + " 个字");
        }
        if (existsName(trimmed, null)) {
            throw BizException.badRequest("同名的吧已经存在了");
        }
        // 前端提示「每人最多创建 3 个吧」
        Long owned = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getOwnerId, userId));
        if (owned != null && owned >= MAX_BA_PER_USER) {
            throw BizException.badRequest("每人最多创建 " + MAX_BA_PER_USER + " 个吧");
        }
        String desc = normalizeDesc(description);

        Category category = new Category();
        category.setName(trimmed);
        // slug 全局唯一，用时间戳兜底，避免用户输入带来重复
        category.setSlug("ba-" + System.currentTimeMillis());
        category.setDescription(desc);
        category.setSortOrder(nextSortOrder());
        category.setStatus(1);
        category.setOwnerId(userId);
        categoryMapper.insert(category);

        return new CategoryVO(category.getId(), category.getName(), category.getSlug(), category.getDescription());
    }

    /** 改吧：仅吧主本人或管理员可改；吧名创建后不可改，只允许改简介 */
    @Transactional
    public void updateCategory(long id, String name, String description, CurrentUser user) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BizException.notFound("这个吧不存在");
        }
        if (!user.isAdmin() && !Objects.equals(category.getOwnerId(), user.userId())) {
            throw BizException.forbidden("只有吧主才能修改吧信息");
        }

        String trimmed = name == null ? "" : name.trim();
        if (!trimmed.isEmpty() && !trimmed.equals(category.getName())) {
            throw BizException.badRequest("吧名创建后不可修改");
        }
        category.setDescription(normalizeDesc(description));
        categoryMapper.updateById(category);
    }

    private boolean existsName(String name, Long excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>().eq(Category::getName, name);
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private String normalizeDesc(String description) {
        String desc = description == null ? "" : description.trim();
        if (desc.length() > DESC_MAX) {
            throw BizException.badRequest("简介最多 " + DESC_MAX + " 个字");
        }
        return desc;
    }

    /** 新吧排在已有吧之后 */
    private int nextSortOrder() {
        Category last = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .orderByDesc(Category::getSortOrder)
                .last("LIMIT 1"));
        int current = last == null || last.getSortOrder() == null ? 0 : last.getSortOrder();
        return current + 1;
    }
}
