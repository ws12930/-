package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.common.PageResult;
import com.fenglin.blog.dto.AdminModels.ReportVO;
import com.fenglin.blog.dto.ReportModels.ReportRequest;
import com.fenglin.blog.entity.Comment;
import com.fenglin.blog.entity.Post;
import com.fenglin.blog.entity.Report;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.CommentMapper;
import com.fenglin.blog.mapper.PostMapper;
import com.fenglin.blog.mapper.ReportMapper;
import com.fenglin.blog.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 举报处理（管理端）。
 *
 * <p>状态：{@code PENDING} 待处理 / {@code HANDLED} 已处理 / {@code IGNORED} 已驳回，
 * 库里对应 0 / 1 / 2。
 */
@Service
public class ReportService {

    public static final String PENDING = "PENDING";
    public static final String HANDLED = "HANDLED";
    public static final String IGNORED = "IGNORED";

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_HANDLED = 1;
    private static final int STATUS_IGNORED = 2;

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final NotificationService notificationService;

    public ReportService(ReportMapper reportMapper, UserMapper userMapper, PostMapper postMapper,
                         CommentMapper commentMapper, NotificationService notificationService) {
        this.reportMapper = reportMapper;
        this.userMapper = userMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.notificationService = notificationService;
    }

    /** 用户提交举报：同一个人对同一目标只能报一次（唯一索引兜底） */
    @Transactional
    public void create(long reporterId, ReportRequest request) {
        String targetType = normalizeTargetType(request.targetType());
        String snapshot = snapshotOf(targetType, request.targetId());
        if (snapshot == null) {
            throw BizException.notFound("举报的内容不存在或已被删除");
        }
        Report report = new Report();
        report.setTargetType(targetType);
        report.setTargetId(request.targetId());
        report.setReporterId(reporterId);
        report.setReason(request.reason().trim());
        report.setTargetSnapshot(snapshot.length() > 120 ? snapshot.substring(0, 120) : snapshot);
        report.setDetail(request.detail() == null ? null : request.detail().trim());
        report.setStatus(STATUS_PENDING);
        report.setCreatedAt(LocalDateTime.now());
        try {
            reportMapper.insert(report);
        } catch (DuplicateKeyException e) {
            throw BizException.badRequest("你已经举报过这条内容了，等管理员处理吧");
        }
    }

    /** 举报列表；status 取 PENDING / HANDLED / IGNORED，空或 ALL 表示全部 */
    public PageResult<ReportVO> list(String status, long current, long size) {
        LambdaQueryWrapper<Report> query = new LambdaQueryWrapper<>();
        Integer value = parseStatus(status, true);
        if (value != null) {
            query.eq(Report::getStatus, value);
        }
        query.orderByAsc(Report::getStatus).orderByDesc(Report::getId);

        IPage<Report> page = reportMapper.selectPage(
                new Page<>(Math.max(1, current), Math.max(1, Math.min(size, 100))), query);

        Map<Long, User> reporters = new HashMap<>();
        List<ReportVO> records = new ArrayList<>(page.getRecords().size());
        for (Report report : page.getRecords()) {
            User reporter = reporters.computeIfAbsent(report.getReporterId(), userMapper::selectById);
            records.add(new ReportVO(
                    report.getId(),
                    report.getReporterId(),
                    reporter == null ? null : (reporter.getNickname() == null
                            ? reporter.getUsername() : reporter.getNickname()),
                    report.getTargetType(),
                    report.getTargetId(),
                    report.getTargetSnapshot(),
                    report.getReason(),
                    report.getDetail(),
                    report.getStatus(),
                    statusText(report.getStatus()),
                    report.getCreatedAt(),
                    report.getHandledAt()));
        }
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), records);
    }

    /** 处理举报：只能处理待处理的单子，处理后给举报人发站内通知 */
    @Transactional
    public void handle(long reportId, String status, long adminId) {
        Integer value = parseStatus(status, false);
        if (value == null || value == STATUS_PENDING) {
            throw BizException.badRequest("处理结果只能是 HANDLED（已处理）或 IGNORED（已驳回）");
        }
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw BizException.notFound("举报不存在");
        }
        if (report.getStatus() != null && report.getStatus() != STATUS_PENDING) {
            throw BizException.badRequest("该举报已经处理过了");
        }
        report.setStatus(value);
        report.setHandlerId(adminId);
        report.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(report);

        notificationService.notify(report.getReporterId(), adminId, NotificationService.TYPE_REPORT,
                null, null, value == STATUS_HANDLED ? "你的举报已处理" : "你的举报已被驳回");
    }

    /** 举报对象类型只接受 POST / COMMENT / USER */
    private String normalizeTargetType(String targetType) {
        String normalized = targetType == null ? "" : targetType.trim().toUpperCase(Locale.ROOT);
        if (!"POST".equals(normalized) && !"COMMENT".equals(normalized) && !"USER".equals(normalized)) {
            throw BizException.badRequest("举报对象类型只支持 POST / COMMENT / USER");
        }
        return normalized;
    }

    /** 取举报时的内容快照；对象不存在返回 null */
    private String snapshotOf(String targetType, Long targetId) {
        switch (targetType) {
            case "POST" -> {
                Post post = postMapper.selectById(targetId);
                return post == null ? null : (post.getTitle() == null ? "（无标题）" : post.getTitle());
            }
            case "COMMENT" -> {
                Comment comment = commentMapper.selectById(targetId);
                if (comment == null) {
                    return null;
                }
                String content = comment.getContent() == null ? "" : comment.getContent().trim();
                return content.length() > 100 ? content.substring(0, 100) + "…" : content;
            }
            default -> {
                User user = userMapper.selectById(targetId);
                if (user == null) {
                    return null;
                }
                return (user.getNickname() == null ? user.getUsername() : user.getNickname())
                        + "（@" + user.getUsername() + "）";
            }
        }
    }

    /** 前端状态名 -> 库里的数字状态；allowAll 时 ALL / 空 返回 null 表示不过滤 */
    private Integer parseStatus(String status, boolean allowAll) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || "ALL".equals(normalized)) {
            if (allowAll) {
                return null;
            }
            throw BizException.badRequest("status 只支持 PENDING / HANDLED / IGNORED");
        }
        return switch (normalized) {
            case PENDING -> STATUS_PENDING;
            case HANDLED -> STATUS_HANDLED;
            case IGNORED -> STATUS_IGNORED;
            default -> throw BizException.badRequest("status 只支持 PENDING / HANDLED / IGNORED");
        };
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "待处理";
        }
        return switch (status) {
            case STATUS_HANDLED -> "已处理";
            case STATUS_IGNORED -> "已驳回";
            default -> "待处理";
        };
    }
}
