package com.fenglin.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 用户侧举报提交。
 */
public final class ReportModels {

    private ReportModels() {
    }

    /**
     * @param targetType POST 文章 / COMMENT 评论 / USER 用户
     * @param targetId   举报对象 id
     * @param reason     举报理由（与后台下拉一致：广告垃圾 / 辱骂攻击 / 违法违规 / 其他）
     * @param detail     补充说明，选填
     */
    public record ReportRequest(
            @NotBlank(message = "缺少举报对象类型") String targetType,

            @NotNull(message = "缺少举报对象") Long targetId,

            @NotBlank(message = "请选择举报理由")
            @Size(max = 20, message = "举报理由过长") String reason,

            @Size(max = 200, message = "补充说明不能超过 200 个字符") String detail) {
    }
}
