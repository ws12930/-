package com.fenglin.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 社区（吧）、签到、自定义表情的请求 / 返回模型。
 */
public final class CommunityModels {

    private CommunityModels() {
    }

    /** 建吧 / 改吧请求 */
    public record CategoryRequest(
            @NotBlank(message = "请输入吧名称")
            @Size(max = 50, message = "吧名称不能超过 50 个字符") String name,

            @Size(max = 200, message = "吧简介不能超过 200 个字符") String description) {
    }

    /**
     * 签到卡片：等级 / 称号 / 经验进度 + 今日签到状态。
     *
     * @param gained 本次签到获得的经验，查询时为 0
     */
    public record CheckinStatus(
            int level,
            String title,
            int exp,
            int levelStartExp,
            int nextLevelExp,
            int streak,
            boolean checked,
            int gained) {
    }

    /** 自定义表情项 */
    public record StickerVO(Long id, String name, String url) {
    }

    /** 新增一个表情 */
    public record StickerRequest(
            @Size(max = 40, message = "表情名不能超过 40 个字符") String name,

            @NotBlank(message = "缺少表情图片地址")
            @Size(max = 500, message = "图片地址过长") String url) {
    }

    /** 批量导入本机缓存的表情 */
    public record StickerImportRequest(
            @NotNull(message = "缺少表情列表") List<StickerRequest> stickers) {
    }
}
