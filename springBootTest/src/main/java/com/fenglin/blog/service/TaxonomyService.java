package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.dto.BlogModels.CategoryVO;
import com.fenglin.blog.dto.BlogModels.TagVO;
import com.fenglin.blog.entity.Category;
import com.fenglin.blog.entity.Tag;
import com.fenglin.blog.mapper.CategoryMapper;
import com.fenglin.blog.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 分类与标签 */
@Service
public class TaxonomyService {

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
}
