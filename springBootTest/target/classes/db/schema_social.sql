-- ============================================================
-- Fenglin Blog —— 社交能力（关注 / 私信）
--
-- 幂等脚本，可重复执行：
--   mysql -u root -p fenglin_blog < src/main/resources/db/schema_social.sql
--
-- 说明：字段补齐用 information_schema 判断 + PREPARE 执行，
-- 避免 MySQL 不支持的 ADD COLUMN IF NOT EXISTS 语法。
-- ============================================================

SET NAMES utf8mb4;

-- ---------------------------- 关注关系 ----------------------------
CREATE TABLE IF NOT EXISTS blog_follow (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    follower_id BIGINT   NOT NULL COMMENT '关注者（粉丝）',
    followee_id BIGINT   NOT NULL COMMENT '被关注者',
    created_at  DATETIME          DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_follow (follower_id, followee_id),
    KEY idx_blog_follow_followee (followee_id),
    KEY idx_blog_follow_follower (follower_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '关注关系';

-- ---------------------------- 私信 ----------------------------
CREATE TABLE IF NOT EXISTS blog_message (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    sender_id        BIGINT        NOT NULL,
    receiver_id      BIGINT        NOT NULL,
    content          VARCHAR(1000) NOT NULL,
    read_flag        TINYINT                 DEFAULT 0 COMMENT '0 未读 / 1 已读',
    sender_deleted   TINYINT                 DEFAULT 0 COMMENT '发送方是否删除会话',
    receiver_deleted TINYINT                 DEFAULT 0 COMMENT '接收方是否删除会话',
    quote_id         BIGINT                  DEFAULT NULL COMMENT '被引用的消息 id',
    recalled         TINYINT      NOT NULL   DEFAULT 0 COMMENT '是否已撤回（双方可见）',
    created_at       DATETIME                DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_blog_message_conversation (sender_id, receiver_id, created_at),
    KEY idx_blog_message_unread (receiver_id, read_flag)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私信';

-- ---------------------------- blog_user 字段兜底 ----------------------------
-- 若库是由旧版 schema.sql 建的，可能缺这几个社交字段，这里补上。
SET @schema := DATABASE();

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'blog_user' AND COLUMN_NAME = 'avatar') > 0,
    'SELECT ''avatar exists''',
    'ALTER TABLE blog_user ADD COLUMN avatar VARCHAR(500) DEFAULT NULL COMMENT ''头像''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'blog_user' AND COLUMN_NAME = 'signature') > 0,
    'SELECT ''signature exists''',
    'ALTER TABLE blog_user ADD COLUMN signature VARCHAR(140) DEFAULT NULL COMMENT ''个性签名''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'blog_user' AND COLUMN_NAME = 'faction') > 0,
    'SELECT ''faction exists''',
    'ALTER TABLE blog_user ADD COLUMN faction VARCHAR(16) DEFAULT ''ninja'' COMMENT ''阵营''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------- blog_message 引用字段兜底 ----------------------------
-- 私信引用（quote_id）是后加的能力，老库补一遍。
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'blog_message' AND COLUMN_NAME = 'quote_id') > 0,
    'SELECT ''quote_id exists''',
    'ALTER TABLE blog_message ADD COLUMN quote_id BIGINT DEFAULT NULL COMMENT ''被引用的消息 id''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------- blog_message 撤回字段兜底 ----------------------------
-- 私信撤回（recalled）是后加的能力，老库补一遍。
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'blog_message' AND COLUMN_NAME = 'recalled') > 0,
    'SELECT ''recalled exists''',
    'ALTER TABLE blog_message ADD COLUMN recalled TINYINT NOT NULL DEFAULT 0 COMMENT ''是否已撤回（双方可见）''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
