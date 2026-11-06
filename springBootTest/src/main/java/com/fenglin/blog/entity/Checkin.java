package com.fenglin.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到记录：(user_id, checkin_date) 唯一，一天一条。
 */
@TableName("blog_checkin")
public class Checkin {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 签到日期（只有日期，不含时间） */
    private LocalDate checkinDate;
    /** 连续签到天数，断签后从 1 重新计 */
    private Integer streak;
    /** 本次获得的经验 */
    private Integer expGained;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getCheckinDate() { return checkinDate; }
    public void setCheckinDate(LocalDate checkinDate) { this.checkinDate = checkinDate; }
    public Integer getStreak() { return streak; }
    public void setStreak(Integer streak) { this.streak = streak; }
    public Integer getExpGained() { return expGained; }
    public void setExpGained(Integer expGained) { this.expGained = expGained; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
