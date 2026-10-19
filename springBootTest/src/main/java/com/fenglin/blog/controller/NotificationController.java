package com.fenglin.blog.controller;

import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.SocialModels.NotificationVO;
import com.fenglin.blog.security.CurrentUserHolder;
import com.fenglin.blog.security.RequireAuth;
import com.fenglin.blog.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RequireAuth
    @GetMapping("/notifications")
    public R<List<NotificationVO>> list() {
        return R.ok(notificationService.list(CurrentUserHolder.require().userId()));
    }

    @RequireAuth
    @GetMapping("/notifications/unread")
    public R<Map<String, Long>> unread() {
        return R.ok(Map.of("count", notificationService.unreadCount(CurrentUserHolder.require().userId())));
    }

    /** 全部标记已读，返回标记条数 */
    @RequireAuth
    @PostMapping("/notifications/read")
    public R<Map<String, Integer>> markAllRead() {
        int updated = notificationService.markAllRead(CurrentUserHolder.require().userId());
        return R.ok("已全部标记为已读", Map.of("updated", updated));
    }

    /** 单条标记已读 */
    @RequireAuth
    @PostMapping("/notifications/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        boolean ok = notificationService.markRead(CurrentUserHolder.require().userId(), id);
        return R.ok(ok ? "已读" : "通知不存在", null);
    }
}
