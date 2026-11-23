package com.fenglin.blog.controller;

import com.fenglin.blog.common.PageResult;
import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.AdminModels.AdminCategoryVO;
import com.fenglin.blog.dto.AdminModels.AdminCheckinVO;
import com.fenglin.blog.dto.AdminModels.AdminCommentVO;
import com.fenglin.blog.dto.AdminModels.AdminMessageVO;
import com.fenglin.blog.dto.AdminModels.AdminUserVO;
import com.fenglin.blog.dto.AdminModels.CategoryEditRequest;
import com.fenglin.blog.dto.AdminModels.CheckinSummary;
import com.fenglin.blog.dto.AdminModels.MessageSummary;
import com.fenglin.blog.dto.AdminModels.ReportVO;
import com.fenglin.blog.dto.BlogModels.PostVO;
import com.fenglin.blog.security.CurrentUserHolder;
import com.fenglin.blog.security.RequireAdmin;
import com.fenglin.blog.service.AdminService;
import com.fenglin.blog.service.PostService;
import com.fenglin.blog.service.ReportService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台：全站文章、评论审核、用户、签到、举报、私信、社区（吧）。整个类都需要管理员权限。
 */
@RestController
@RequestMapping("/api/admin")
@RequireAdmin
public class AdminController {

    private final PostService postService;
    private final AdminService adminService;
    private final ReportService reportService;

    public AdminController(PostService postService, AdminService adminService, ReportService reportService) {
        this.postService = postService;
        this.adminService = adminService;
        this.reportService = reportService;
    }

    /** 全站文章（含草稿） */
    @GetMapping("/posts")
    public R<PageResult<PostVO>> posts(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return R.ok(postService.listAll(current, size, keyword, status));
    }

    /** 用户列表 */
    @GetMapping("/users")
    public R<PageResult<AdminUserVO>> users(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return R.ok(adminService.listUsers(current, size, keyword));
    }

    /** 启用 / 禁用账号 */
    @PutMapping("/users/{userId}/status")
    public R<Void> setUserStatus(@PathVariable long userId, @RequestParam int status) {
        adminService.setUserStatus(userId, status, CurrentUserHolder.require().userId());
        return R.ok(status == 1 ? "账号已启用" : "账号已禁用", null);
    }

    /** 升级 / 取消管理员权限：role 取 ADMIN / USER */
    @PutMapping("/users/{userId}/role")
    public R<Void> setUserRole(@PathVariable long userId, @RequestParam String role) {
        adminService.setUserRole(userId, role, CurrentUserHolder.require().userId());
        boolean admin = "ADMIN".equalsIgnoreCase(role);
        return R.ok(admin ? "已升级为管理员" : "已取消管理员权限", null);
    }

    /** 举报列表 */
    @GetMapping("/reports")
    public R<PageResult<ReportVO>> reports(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        return R.ok(reportService.list(status, current, size));
    }

    /** 处理举报：HANDLED 已处理 / IGNORED 已忽略 */
    @PutMapping("/reports/{reportId}/status")
    public R<Void> handleReport(@PathVariable long reportId, @RequestParam String status) {
        reportService.handle(reportId, status, CurrentUserHolder.require().userId());
        return R.ok("举报已处理", null);
    }

    /* ==================== 评论审核 ==================== */

    /** 评论列表：status 取 ALL / NORMAL / HIDDEN，keyword 匹配内容 */
    @GetMapping("/comments")
    public R<PageResult<AdminCommentVO>> comments(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return R.ok(adminService.listComments(current, size, keyword, status));
    }

    /** 审核评论：HIDDEN 隐藏下架 / NORMAL 恢复展示 */
    @PutMapping("/comments/{commentId}/status")
    public R<Void> setCommentStatus(@PathVariable long commentId, @RequestParam String status) {
        adminService.setCommentStatus(commentId, status);
        boolean hidden = "HIDDEN".equalsIgnoreCase(status);
        return R.ok(hidden ? "评论已隐藏" : "评论已恢复", null);
    }

    /** 删除评论 */
    @DeleteMapping("/comments/{commentId}")
    public R<Void> deleteComment(@PathVariable long commentId) {
        adminService.deleteComment(commentId);
        return R.ok("评论已删除", null);
    }

    /* ==================== 签到 ==================== */

    /** 签到记录：keyword 匹配昵称 / 用户名 */
    @GetMapping("/checkins")
    public R<PageResult<AdminCheckinVO>> checkins(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return R.ok(adminService.listCheckins(current, size, keyword));
    }

    /** 签到概览 */
    @GetMapping("/checkins/summary")
    public R<CheckinSummary> checkinSummary() {
        return R.ok(adminService.checkinSummary());
    }

    /* ==================== 私信管理 ==================== */

    /** 全站私信：keyword 匹配正文或双方昵称 / 用户名 */
    @GetMapping("/messages")
    public R<PageResult<AdminMessageVO>> messages(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return R.ok(adminService.listMessages(current, size, keyword));
    }

    /** 私信概览：总条数 / 今日新增 / 未读 */
    @GetMapping("/messages/summary")
    public R<MessageSummary> messageSummary() {
        return R.ok(adminService.messageSummary());
    }

    /** 删除一条私信（违规内容处理） */
    @DeleteMapping("/messages/{messageId}")
    public R<Void> deleteMessage(@PathVariable long messageId) {
        adminService.deleteMessage(messageId);
        return R.ok("私信已删除", null);
    }

    /* ==================== 社区（吧）管理 ==================== */

    /** 全部吧：含吧主、帖子数与启用状态 */
    @GetMapping("/categories")
    public R<PageResult<AdminCategoryVO>> categories(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return R.ok(adminService.listCategories(current, size, keyword));
    }

    /** 启用 / 停用吧 */
    @PutMapping("/categories/{categoryId}/status")
    public R<Void> setCategoryStatus(@PathVariable long categoryId, @RequestParam int status) {
        adminService.setCategoryStatus(categoryId, status);
        return R.ok(status == 1 ? "吧已启用" : "吧已停用", null);
    }

    /** 修改吧信息：管理员可以改名 */
    @PutMapping("/categories/{categoryId}")
    public R<Void> updateCategory(@PathVariable long categoryId, @RequestBody CategoryEditRequest request) {
        adminService.updateCategory(categoryId, request.name(), request.description());
        return R.ok("吧信息已更新", null);
    }

    /** 删除吧：吧下还有帖子时不允许删除 */
    @DeleteMapping("/categories/{categoryId}")
    public R<Void> deleteCategory(@PathVariable long categoryId) {
        adminService.deleteCategory(categoryId);
        return R.ok("吧已删除", null);
    }
}
