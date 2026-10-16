package com.fenglin.blog.controller;

import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.BlogModels.CommentRequest;
import com.fenglin.blog.dto.BlogModels.CommentVO;
import com.fenglin.blog.security.RequireAuth;
import com.fenglin.blog.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/posts/{postId}/comments")
    public R<List<CommentVO>> list(@PathVariable Long postId) {
        return R.ok(commentService.list(postId));
    }

    @RequireAuth
    @PostMapping("/posts/{postId}/comments")
    public R<CommentVO> create(@PathVariable Long postId, @Valid @RequestBody CommentRequest request) {
        return R.ok("评论已发布", commentService.create(postId, request));
    }

    @RequireAuth
    @DeleteMapping("/comments/{commentId}")
    public R<Void> delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
        return R.ok("评论已删除", null);
    }
}
