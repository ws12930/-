package com.fenglin.blog.controller;

import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.BlogModels.CategoryVO;
import com.fenglin.blog.dto.BlogModels.TagVO;
import com.fenglin.blog.dto.CommunityModels.CategoryRequest;
import com.fenglin.blog.security.CurrentUserHolder;
import com.fenglin.blog.security.RequireAuth;
import com.fenglin.blog.service.TaxonomyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** 建吧：登录用户即可创建，创建者是吧主 */
    @RequireAuth
    @PostMapping("/categories")
    public R<CategoryVO> createCategory(@RequestBody CategoryRequest request) {
        long userId = CurrentUserHolder.require().userId();
        return R.ok("吧创建成功", taxonomyService.createCategory(request.name(), request.description(), userId));
    }

    /** 改吧：仅吧主或管理员 */
    @RequireAuth
    @PutMapping("/categories/{id}")
    public R<Void> updateCategory(@PathVariable long id, @RequestBody CategoryRequest request) {
        taxonomyService.updateCategory(id, request.name(), request.description(), CurrentUserHolder.require());
        return R.ok("吧信息已更新", null);
    }

    @GetMapping("/tags")
    public R<List<TagVO>> tags() {
        return R.ok(taxonomyService.listTags());
    }
}
