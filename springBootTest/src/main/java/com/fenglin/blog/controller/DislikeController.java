package com.fenglin.blog.controller;

import com.fenglin.blog.common.R;
import com.fenglin.blog.security.RequireAuth;
import com.fenglin.blog.service.DislikeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 不感兴趣：帖子 / 吧两级，仅影响当前用户自己的信息流 */
@RestController
@RequestMapping("/api/dislikes")
public class DislikeController {

    private final DislikeService dislikeService;

    public DislikeController(DislikeService dislikeService) {
        this.dislikeService = dislikeService;
    }

    @RequireAuth
    @GetMapping
    public R<Map<String, Object>> mine() {
        return R.ok(dislikeService.listMine());
    }

    @RequireAuth
    @PostMapping("/posts/{postId}")
    public R<Void> markPost(@PathVariable Long postId) {
        dislikeService.markPost(postId);
        return R.ok("已减少此类内容推荐", null);
    }

    @RequireAuth
    @DeleteMapping("/posts/{postId}")
    public R<Void> unmarkPost(@PathVariable Long postId) {
        dislikeService.unmarkPost(postId);
        return R.ok("已撤销", null);
    }

    @RequireAuth
    @PostMapping("/bas/{categoryId}")
    public R<Void> markBa(@PathVariable Long categoryId) {
        dislikeService.markBa(categoryId);
        return R.ok("已屏蔽该吧", null);
    }

    @RequireAuth
    @DeleteMapping("/bas/{categoryId}")
    public R<Void> unmarkBa(@PathVariable Long categoryId) {
        dislikeService.unmarkBa(categoryId);
        return R.ok("已撤销", null);
    }
}
