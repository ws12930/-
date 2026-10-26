package com.fenglin.blog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class BlogModels {

    private BlogModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PostRequest(
            @NotNull(message = "请选择文章分类") Long categoryId,
            @NotBlank(message = "请输入文章标题")
            @Size(max = 150, message = "标题不能超过 150 个字符") String title,
            @Size(max = 120, message = "slug 不能超过 120 个字符") String slug,
            @Size(max = 500, message = "摘要不能超过 500 个字符") String summary,
            @NotBlank(message = "请输入文章正文") String content,
            @Size(max = 500, message = "封面地址不能超过 500 个字符") String coverUrl,
            /** 文章状态：不做枚举限制，任意字符串均可；只有 DRAFT 被视为草稿（对外不可见） */
            @Size(max = 16, message = "状态不能超过 16 个字符") String status,
            /** 是否置顶：true/1 置顶，其余视为不置顶 */
            Boolean pinned,
            List<Long> tagIds) {
    }

    /** 置顶切换请求 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PinRequest(Boolean pinned) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommentRequest(
            Long parentId,
            @NotBlank(message = "请输入评论内容")
            @Size(max = 1000, message = "评论不能超过 1000 个字符") String content) {
    }

    public record CategoryVO(Long id, String name, String slug, String description) {
    }

    public record TagVO(Long id, String name, String slug) {
    }

    public record PostVO(
            Long id,
            Long authorId,
            String authorName,
            String authorAvatar,
            String authorFaction,
            Long categoryId,
            String categoryName,
            String title,
            String slug,
            String summary,
            String content,
            String coverUrl,
            String status,
            Long viewCount,
            Long likeCount,
            Long commentCount,
            Boolean liked,
            Boolean pinned,
            LocalDateTime pinnedAt,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<TagVO> tags) {
    }

    public record CommentVO(
            Long id,
            Long postId,
            Long userId,
            String userName,
            String userAvatar,
            String userFaction,
            Long parentId,
            Long replyToUserId,
            String replyToName,
            String content,
            LocalDateTime createdAt) {
    }
}
