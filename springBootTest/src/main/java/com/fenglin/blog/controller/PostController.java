package com.fenglin.blog.controller;

import com.fenglin.blog.common.PageResult;
import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.BlogModels.PinRequest;
import com.fenglin.blog.dto.BlogModels.PostRequest;
import com.fenglin.blog.dto.BlogModels.PostVO;
import com.fenglin.blog.security.RequireAuth;
import com.fenglin.blog.service.LikeService;
import com.fenglin.blog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final LikeService likeService;

    public PostController(PostService postService, LikeService likeService) {
        this.postService = postService;
        this.likeService = likeService;
    }

    @GetMapping
    public R<PageResult<PostVO>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId) {
        return R.ok(postService.listPublished(current, size, keyword, categoryId, tagId));
    }

    @GetMapping("/slug/{slug}")
    public R<PostVO> detail(@PathVariable String slug) {
        return R.ok(postService.getPublishedBySlug(slug));
    }

    @RequireAuth
    @GetMapping("/mine")
    public R<PageResult<PostVO>> mine(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        return R.ok(postService.listMine(current, size, status));
    }

    @RequireAuth
    @PostMapping
    public R<PostVO> create(@Valid @RequestBody PostRequest request) {
        return R.ok("文章已保存", postService.create(request));
    }

    @RequireAuth
    @PutMapping("/{postId}")
    public R<PostVO> update(@PathVariable Long postId, @Valid @RequestBody PostRequest request) {
        return R.ok("文章已更新", postService.update(postId, request));
    }

    @RequireAuth
    @DeleteMapping("/{postId}")
    public R<Void> delete(@PathVariable Long postId) {
        postService.delete(postId);
        return R.ok("文章已删除", null);
    }

    /** 置顶 / 取消置顶：作者本人或管理员可操作 */
    @RequireAuth
    @PostMapping("/{postId}/pin")
    public R<PostVO> togglePin(@PathVariable Long postId, @RequestBody(required = false) PinRequest request) {
        boolean pinned = request != null && Boolean.TRUE.equals(request.pinned());
        return R.ok(pinned ? "已置顶" : "已取消置顶", postService.togglePin(postId, pinned));
    }

    /** 点赞 / 取消点赞切换 */
    @RequireAuth
    @PostMapping("/{postId}/like")
    public R<Map<String, Object>> toggleLike(@PathVariable Long postId) {
        var result = likeService.toggle(postId);
        return R.ok(result.liked() ? "已点赞" : "已取消点赞",
                Map.of("postId", result.postId(), "likeCount", result.likeCount(), "liked", result.liked()));
    }
}
