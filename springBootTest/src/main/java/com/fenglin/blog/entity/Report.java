package com.fenglin.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 内容举报：POST / COMMENT，(target_type, target_id, reporter_id) 唯一。
 */
@TableName("blog_report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 举报对象类型：POST / COMMENT */
    private String targetType;
    private Long targetId;
    private Long reporterId;
    /** 举报理由 */
    private String reason;
    /** 举报时的内容快照（标题或评论摘要） */
    private String targetSnapshot;
    /** 补充说明 */
    private String detail;
    /** 0 待处理 / 1 已处理 / 2 已驳回 */
    private Integer status;
    private Long handlerId;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getTargetSnapshot() { return targetSnapshot; }
    public void setTargetSnapshot(String targetSnapshot) { this.targetSnapshot = targetSnapshot; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getHandlerId() { return handlerId; }
    public void setHandlerId(Long handlerId) { this.handlerId = handlerId; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
