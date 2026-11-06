package com.fenglin.blog.controller;

import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.CommunityModels.StickerImportRequest;
import com.fenglin.blog.dto.CommunityModels.StickerRequest;
import com.fenglin.blog.dto.CommunityModels.StickerVO;
import com.fenglin.blog.security.CurrentUserHolder;
import com.fenglin.blog.security.RequireAuth;
import com.fenglin.blog.service.StickerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自定义表情：所有接口都需要登录，且只能操作自己的表情。
 * 增删都直接返回最新列表，前端拿到后覆盖本地缓存即可。
 */
@RestController
@RequestMapping("/api")
public class StickerController {

    private final StickerService stickerService;

    public StickerController(StickerService stickerService) {
        this.stickerService = stickerService;
    }

    @RequireAuth
    @GetMapping("/stickers")
    public R<List<StickerVO>> list() {
        return R.ok(stickerService.list(CurrentUserHolder.require().userId()));
    }

    @RequireAuth
    @PostMapping("/stickers")
    public R<List<StickerVO>> add(@Valid @RequestBody StickerRequest request) {
        return R.ok("表情已添加", stickerService.add(
                CurrentUserHolder.require().userId(), request.name(), request.url()));
    }

    /** 把本机缓存的表情批量导入账号 */
    @RequireAuth
    @PostMapping("/stickers/import")
    public R<List<StickerVO>> importAll(@Valid @RequestBody StickerImportRequest request) {
        return R.ok(stickerService.importAll(CurrentUserHolder.require().userId(), request.stickers()));
    }

    @RequireAuth
    @DeleteMapping("/stickers/{stickerId}")
    public R<List<StickerVO>> delete(@PathVariable long stickerId) {
        return R.ok("表情已删除", stickerService.delete(
                CurrentUserHolder.require().userId(), stickerId));
    }
}
