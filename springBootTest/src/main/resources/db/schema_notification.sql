-- ============================================================
-- Fenglin Blog —— 点赞与通知
--
-- 幂等脚本，可重复执行：
--   mysql -u root -p fenglin_blog < src/main/resources/db/schema_notification.sql
-- ============================================================

SET NAMES utf8mb4;

-- ---------------------------- 文章点赞 ----------------------------
CREATE TABLE IF NOT EXISTS blog_post_like (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    post_id    BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    created_at DATETIME          DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_post_like (post_id, user_id),
    KEY idx_blog_post_like_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章点赞';

-- ---------------------------- 站内通知 ----------------------------
-- type：LIKE 点赞文章 / COMMENT 评论文章 / REPLY 回复评论 / FOLLOW 关注我
CREATE TABLE IF NOT EXISTS blog_notification (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL COMMENT '通知接收者',
    actor_id   BIGINT      NOT NULL COMMENT '触发者',
    type       VARCHAR(20) NOT NULL COMMENT 'LIKE / COMMENT / REPLY / FOLLOW',
    post_id    BIGINT               DEFAULT NULL,
    comment_id BIGINT               DEFAULT NULL,
    content    VARCHAR(200)         DEFAULT NULL COMMENT '摘要文本（评论内容预览等）',
    read_flag  TINYINT              DEFAULT 0 COMMENT '0 未读 / 1 已读',
    created_at DATETIME             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_blog_notification_user (user_id, read_flag, created_at),
    KEY idx_blog_notification_actor (actor_id, type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '站内通知';
