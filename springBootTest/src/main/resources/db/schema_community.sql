-- ============================================================
-- Fenglin Blog —— 社区广场：不感兴趣（帖子 / 吧）
--
-- 幂等脚本，可重复执行：
--   mysql -u root -p fenglin_blog < src/main/resources/db/schema_community.sql
--
-- 说明：记录"某用户对某帖子 / 某吧不感兴趣"，仅影响该用户自己的信息流，
-- 不改动帖子或吧本身；用户可随时撤销（删除记录）。
-- ============================================================

SET NAMES utf8mb4;

-- ---------------------------- 帖子不感兴趣 ----------------------------
CREATE TABLE IF NOT EXISTS blog_post_dislike (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL COMMENT '用户 id',
    post_id    BIGINT   NOT NULL COMMENT '帖子 id',
    created_at DATETIME          DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_post_dislike (user_id, post_id),
    KEY idx_blog_post_dislike_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子不感兴趣';

-- ---------------------------- 吧不感兴趣 ----------------------------
CREATE TABLE IF NOT EXISTS blog_ba_dislike (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL COMMENT '用户 id',
    category_id BIGINT   NOT NULL COMMENT '吧（分类）id',
    created_at  DATETIME          DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_ba_dislike (user_id, category_id),
    KEY idx_blog_ba_dislike_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '吧不感兴趣';
