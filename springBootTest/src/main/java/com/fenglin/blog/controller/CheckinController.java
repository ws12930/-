package com.fenglin.blog.controller;

import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.CommunityModels.CheckinStatus;
import com.fenglin.blog.security.CurrentUserHolder;
import com.fenglin.blog.security.RequireAuth;
import com.fenglin.blog.service.CheckinService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台签到：卡片状态与签到动作。签到记录的管理端视图在 {@link AdminController}。
 */
@RestController
@RequestMapping("/api")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    /** 今日签到状态 + 等级 / 经验进度 */
    @RequireAuth
    @GetMapping("/checkin/today")
    public R<CheckinStatus> today() {
        return R.ok(checkinService.status(CurrentUserHolder.require().userId()));
    }

    /** 签到（+5 经验），重复签到返回业务错误 */
    @RequireAuth
    @PostMapping("/checkin")
    public R<CheckinStatus> checkin() {
        CheckinStatus status = checkinService.checkin(CurrentUserHolder.require().userId());
        return R.ok("签到成功，经验 +" + status.gained(), status);
    }
}
