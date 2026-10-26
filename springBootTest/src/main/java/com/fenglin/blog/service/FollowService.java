package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.dto.SocialModels.UserProfileVO;
import com.fenglin.blog.entity.Follow;
import com.fenglin.blog.mapper.FollowMapper;
import com.fenglin.blog.security.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 关注 / 取关 */
@Service
public class FollowService {

    private final FollowMapper followMapper;
    private final UserService userService;
    private final NotificationService notificationService;

    public FollowService(FollowMapper followMapper, UserService userService,
                         NotificationService notificationService) {
        this.followMapper = followMapper;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional
    public FollowResult toggle(Long followeeId) {
        Long followerId = CurrentUserHolder.require().userId();
        if (followerId.equals(followeeId)) {
            throw BizException.badRequest("不能关注自己");
        }
        if (userService.profile(followeeId) == null) {
            throw BizException.notFound("用户不存在");
        }

        Follow existing = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFolloweeId, followeeId));

        boolean followed;
        if (existing == null) {
            Follow record = new Follow();
            record.setFollowerId(followerId);
            record.setFolloweeId(followeeId);
            record.setCreatedAt(LocalDateTime.now());
            followMapper.insert(record);
            followed = true;
        } else {
            followMapper.deleteById(existing.getId());
            followed = false;
        }

        long followerCount = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, followeeId));

        // 互相关注：我关注了 TA，且 TA 也关注了我
        boolean mutual = followed && followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followeeId)
                .eq(Follow::getFolloweeId, followerId)) > 0;

        if (followed) {
            notificationService.notify(followeeId, followerId, NotificationService.TYPE_FOLLOW,
                    null, null, null);
        } else {
            notificationService.revoke(followeeId, followerId, NotificationService.TYPE_FOLLOW, null);
        }

        return new FollowResult(followed, followerCount, mutual);
    }

    public List<UserProfileVO> listFollowing(Long userId) {
        List<Follow> follows = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .orderByDesc(Follow::getCreatedAt));
        List<UserProfileVO> list = new ArrayList<>(follows.size());
        for (Follow follow : follows) {
            list.add(userService.profile(follow.getFolloweeId()));
        }
        return list;
    }

    public List<UserProfileVO> listFollowers(Long userId) {
        List<Follow> follows = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, userId)
                .orderByDesc(Follow::getCreatedAt));
        List<UserProfileVO> list = new ArrayList<>(follows.size());
        for (Follow follow : follows) {
            list.add(userService.profile(follow.getFollowerId()));
        }
        return list;
    }

    public record FollowResult(boolean followed, long followerCount, boolean mutual) {
    }
}
