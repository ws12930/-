package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.dto.CommunityModels.CheckinStatus;
import com.fenglin.blog.entity.Checkin;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.CheckinMapper;
import com.fenglin.blog.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到。
 *
 * <p>一天一条（{@code uk_checkin_user_date} 兜底），连续天数靠「昨天有没有签」推导：
 * 昨天签过就在原 streak 上 +1，否则从 1 重新算。
 */
@Service
public class CheckinService {

    private final CheckinMapper checkinMapper;
    private final UserMapper userMapper;
    private final LevelService levelService;

    public CheckinService(CheckinMapper checkinMapper, UserMapper userMapper, LevelService levelService) {
        this.checkinMapper = checkinMapper;
        this.userMapper = userMapper;
        this.levelService = levelService;
    }

    /** 今日签到状态 + 等级信息 */
    public CheckinStatus status(long userId) {
        LocalDate today = LocalDate.now();
        Checkin todayRecord = find(userId, today);
        int streak = todayRecord != null ? value(todayRecord.getStreak()) : currentStreak(userId, today);
        return build(userId, todayRecord != null, streak, 0);
    }

    /** 签到：已经签过则拒绝，成功返回最新状态 */
    @Transactional
    public CheckinStatus checkin(long userId) {
        LocalDate today = LocalDate.now();
        if (find(userId, today) != null) {
            throw BizException.badRequest("今天已经签到过了，明天再来");
        }
        Checkin yesterday = find(userId, today.minusDays(1));
        int streak = yesterday == null ? 1 : value(yesterday.getStreak()) + 1;

        Checkin record = new Checkin();
        record.setUserId(userId);
        record.setCheckinDate(today);
        record.setStreak(streak);
        record.setExpGained(LevelService.EXP_CHECKIN);
        record.setCreatedAt(LocalDateTime.now());
        try {
            checkinMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发双击：唯一索引兜底，按「已签到」处理
            throw BizException.badRequest("今天已经签到过了，明天再来");
        }

        levelService.addExp(userId, LevelService.EXP_CHECKIN);
        return build(userId, true, streak, LevelService.EXP_CHECKIN);
    }

    private CheckinStatus build(long userId, boolean checked, int streak, int gained) {
        User user = userMapper.selectById(userId);
        int exp = levelService.expOf(user);
        int level = levelService.levelOf(exp);
        return new CheckinStatus(
                level,
                levelService.titleOf(exp),
                exp,
                levelService.levelStartExp(level),
                levelService.nextLevelExp(level),
                streak,
                checked,
                gained);
    }

    private Checkin find(long userId, LocalDate date) {
        return checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getCheckinDate, date)
                .last("LIMIT 1"));
    }

    /** 今天还没签：显示的是「昨天为止」的连续天数 */
    private int currentStreak(long userId, LocalDate today) {
        Checkin yesterday = find(userId, today.minusDays(1));
        return yesterday == null ? 0 : value(yesterday.getStreak());
    }

    private int value(Integer n) {
        return n == null ? 0 : n;
    }
}
