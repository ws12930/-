package com.fenglin.blog.controller;

import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.BlogModels.CategoryVO;
import com.fenglin.blog.dto.BlogModels.TagVO;
import com.fenglin.blog.service.TaxonomyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaxonomyController {

    private final TaxonomyService taxonomyService;

    public TaxonomyController(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    @GetMapping("/categories")
    public R<List<CategoryVO>> categories() {
        return R.ok(taxonomyService.listCategories());
    }

    @GetMapping("/tags")
    public R<List<TagVO>> tags() {
        return R.ok(taxonomyService.listTags());
    }
}
