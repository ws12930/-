package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 等级 / 经验。
 *
 * <p>经验值统一存在 {@code blog_user.exp}，等级与称号一律由经验推导，不落库，
 * 这样调曲线时不用刷数据。升级门槛：Lv N 需要 {@code 50 * (N-1) * N} 点累计经验
 * （Lv1 = 0，Lv2 = 100，Lv3 = 300，Lv4 = 600 …）。
 */
@Service
public class LevelService {

    /** 发布文章（含草稿转发布）获得经验 */
    public static final int EXP_POST = 20;
    /** 发表评论 / 回复获得经验 */
    public static final int EXP_COMMENT = 5;
    /** 每日签到获得经验 */
    public static final int EXP_CHECKIN = 5;

    private static final int MAX_LEVEL = 12;
    /** 升级曲线系数：Lv(N+1) 门槛 = 50 * N * (N+1) */
    private static final int CURVE = 50;

    /** 下标 = 等级 - 1 */
    private static final String[] TITLES = {
            "见习生", "修行者", "笔锋客", "编外席官",
            "席官", "副队长", "队长", "卍解者",
            "影级", "传说", "七时守望", "七时之主"
    };

    private final UserMapper userMapper;

    public LevelService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 用户的累计经验，空值按 0 处理 */
    public int expOf(User user) {
        if (user == null || user.getExp() == null) {
            return 0;
        }
        return Math.max(0, user.getExp());
    }

    /** 经验 -> 等级（1 ~ {@value #MAX_LEVEL}） */
    public int levelOf(int exp) {
        int level = 1;
        while (level < MAX_LEVEL && exp >= levelStartExp(level + 1)) {
            level++;
        }
        return level;
    }

    /** 该等级的起始累计经验 */
    public int levelStartExp(int level) {
        int n = Math.max(1, Math.min(level, MAX_LEVEL));
        return CURVE * (n - 1) * n;
    }

    /** 升到下一级所需的累计经验；已满级返回 0 */
    public int nextLevelExp(int level) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return levelStartExp(level + 1);
    }

    /** 经验 -> 称号 */
    public String titleOf(int exp) {
        return TITLES[levelOf(exp) - 1];
    }

    /** 增加经验：直接走 SQL 自增，避免并发下读改写丢更新 */
    @Transactional
    public void addExp(long userId, int exp) {
        if (exp <= 0) {
            return;
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .setSql("exp = exp + " + exp)
                .eq(User::getId, userId));
    }
}
