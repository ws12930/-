package com.fenglin.blog.controller;

import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.ReportModels.ReportRequest;
import com.fenglin.blog.security.CurrentUserHolder;
import com.fenglin.blog.security.RequireAuth;
import com.fenglin.blog.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台举报提交。管理端查看 / 处理在 {@link AdminController} 的 /api/admin/reports。
 */
@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @RequireAuth
    @PostMapping("/reports")
    public R<Void> create(@Valid @RequestBody ReportRequest request) {
        reportService.create(CurrentUserHolder.require().userId(), request);
        return R.ok("举报已提交，管理员会尽快处理", null);
    }
}
