package com.fenglin.blog.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端专用返回模型。
 */
public final class AdminModels {

    private AdminModels() {
    }

    /** 举报列表项；status 为 0 待处理 / 1 已处理 / 2 已驳回，statusText 为对应中文 */
    public record ReportVO(
            Long id,
            Long reporterId,
            String reporterName,
            String targetType,
            Long targetId,
            String targetSnapshot,
            String reason,
            String detail,
            Integer status,
            String statusText,
            LocalDateTime createdAt,
            LocalDateTime handledAt) {
    }

    /** 用户管理列表项 */
    public record AdminUserVO(
            Long id,
            String username,
            String nickname,
            String email,
            String role,
            Integer status,
            int exp,
            int level,
            String title,
            LocalDateTime createdAt) {
    }

    /** 评论审核列表项；status 为 NORMAL 正常 / HIDDEN 已隐藏 */
    public record AdminCommentVO(
            Long id,
            Long postId,
            String postTitle,
            String postSlug,
            Long userId,
            String authorName,
            String content,
            String status,
            boolean hidden,
            LocalDateTime createdAt) {
    }

    /** 签到记录列表项 */
    public record AdminCheckinVO(
            Long id,
            Long userId,
            String userName,
            String nickname,
            LocalDate checkinDate,
            Integer streak,
            Integer expGained,
            LocalDateTime createdAt) {
    }

    /** 签到概览：今日人数 / 今日次数 / 累计次数 / 最长连续 */
    public record CheckinSummary(
            long todayUsers,
            long todayCount,
            long totalCount,
            int maxStreak) {
    }

    /** 私信管理列表项；recalled 为 1 时表示已撤回，正文已清空 */
    public record AdminMessageVO(
            Long id,
            Long senderId,
            String senderName,
            Long receiverId,
            String receiverName,
            String content,
            Integer readFlag,
            Integer recalled,
            LocalDateTime createdAt) {
    }

    /** 私信概览：总条数 / 今日新增 / 未读条数 */
    public record MessageSummary(
            long total,
            long today,
            long unread) {
    }

    /** 社区（吧）管理列表项 */
    public record AdminCategoryVO(
            Long id,
            String name,
            String slug,
            String description,
            Long ownerId,
            String ownerName,
            long postCount,
            Integer status,
            Integer sortOrder,
            LocalDateTime createdAt) {
    }

    /** 管理端修改吧信息（管理员可改名，前台吧主只能改简介） */
    public record CategoryEditRequest(
            String name,
            String description) {
    }
}
