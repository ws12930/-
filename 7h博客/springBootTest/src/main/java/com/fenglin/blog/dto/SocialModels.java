package com.fenglin.blog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 社交能力：资料、关注、私信相关的数据模型 */
public final class SocialModels {

    private SocialModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfileUpdateRequest(
            @Size(max = 50, message = "昵称不能超过 50 个字符") String nickname,
            @Size(max = 500, message = "头像标识不能超过 500 个字符") String avatar,
            @Size(max = 140, message = "个性签名不能超过 140 个字符") String signature,
            @Size(max = 16, message = "阵营标识非法") String faction) {
    }

    /** 用户公开资料（不含邮箱等敏感字段） */
    public record UserProfileVO(
            Long id,
            String username,
            String nickname,
            String avatar,
            String signature,
            String faction,
            LocalDateTime createdAt,
            Long postCount,
            Long followerCount,
            Long followingCount,
            Boolean followed,
            Boolean self,
            Boolean mutual) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageSendRequest(
            @Size(max = 1000, message = "消息不能超过 1000 个字符") @NotBlank(message = "请输入消息内容") String content) {
    }

    public record MessageVO(
            Long id,
            Long senderId,
            Long receiverId,
            String content,
            Integer readFlag,
            LocalDateTime createdAt) {
    }

    /** 会话列表项：与某个用户的最近一条消息 + 未读条数 */
    public record ConversationVO(
            Long peerId,
            String peerName,
            String peerAvatar,
            String peerFaction,
            String lastMessage,
            LocalDateTime lastAt,
            Long unreadCount) {
    }

    /** 通知列表项：谁对「我的文章/评论」做了什么 */
    public record NotificationVO(
            Long id,
            String type,
            Long actorId,
            String actorName,
            String actorAvatar,
            String actorFaction,
            Long postId,
            String postTitle,
            String postSlug,
            Long commentId,
            String content,
            Integer readFlag,
            LocalDateTime createdAt) {
    }
}
