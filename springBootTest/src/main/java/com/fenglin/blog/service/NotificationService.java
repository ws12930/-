package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.dto.SocialModels.NotificationVO;
import com.fenglin.blog.entity.Notification;
import com.fenglin.blog.entity.Post;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.NotificationMapper;
import com.fenglin.blog.mapper.PostMapper;
import com.fenglin.blog.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 站内通知。
 *
 * <p>类型：{@code LIKE} 点赞文章、{@code COMMENT} 评论文章、
 * {@code REPLY} 回复评论、{@code FOLLOW} 关注我。
 */
@Service
public class NotificationService {

    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_REPLY = "REPLY";
    public static final String TYPE_FOLLOW = "FOLLOW";

    private static final long DEFAULT_LIMIT = 100L;

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final PostMapper postMapper;

    public NotificationService(NotificationMapper notificationMapper,
                               UserMapper userMapper,
                               PostMapper postMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.postMapper = postMapper;
    }

    public List<NotificationVO> list(Long userId) {
        List<Notification> records = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt)
                        .last("LIMIT " + DEFAULT_LIMIT));
        List<NotificationVO> list = new ArrayList<>(records.size());
        for (Notification record : records) {
            list.add(toVO(record));
        }
        return list;
    }

    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, 0));
    }

    @Transactional
    public int markAllRead(Long userId) {
        Notification update = new Notification();
        update.setReadFlag(1);
        return notificationMapper.update(update, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, 0));
    }

    @Transactional
    public boolean markRead(Long userId, Long id) {
        Notification update = new Notification();
        update.setReadFlag(1);
        return notificationMapper.update(update, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)) > 0;
    }

    /**
     * 写入一条通知。
     *
     * <p>自己触发的动作不通知自己；同一人对同一目标的同类型通知会被替换成最新一条，
     * 避免反复点赞 / 反复关注刷屏。
     */
    @Transactional
    public void notify(Long userId, Long actorId, String type, Long postId, Long commentId, String content) {
        if (userId == null || actorId == null || userId.equals(actorId)) {
            return;
        }
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getActorId, actorId)
                .eq(Notification::getType, type)
                .eq(postId != null, Notification::getPostId, postId)
                .eq(commentId != null, Notification::getCommentId, commentId));

        Notification record = new Notification();
        record.setUserId(userId);
        record.setActorId(actorId);
        record.setType(type);
        record.setPostId(postId);
        record.setCommentId(commentId);
        record.setContent(content);
        record.setReadFlag(0);
        record.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(record);
    }

    /** 删除某人对某目标的通知（取消点赞 / 取关时用） */
    @Transactional
    public void revoke(Long userId, Long actorId, String type, Long postId) {
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getActorId, actorId)
                .eq(Notification::getType, type)
                .eq(postId != null, Notification::getPostId, postId));
    }

    private NotificationVO toVO(Notification record) {
        User actor = record.getActorId() == null ? null : userMapper.selectById(record.getActorId());
        Post post = record.getPostId() == null ? null : postMapper.selectById(record.getPostId());
        return new NotificationVO(
                record.getId(),
                record.getType(),
                record.getActorId(),
                actor == null ? null : actor.getNickname(),
                actor == null ? null : actor.getAvatar(),
                actor == null ? null : actor.getFaction(),
                record.getPostId(),
                post == null ? null : post.getTitle(),
                post == null ? null : post.getSlug(),
                record.getCommentId(),
                record.getContent(),
                record.getReadFlag(),
                record.getCreatedAt());
    }
}
