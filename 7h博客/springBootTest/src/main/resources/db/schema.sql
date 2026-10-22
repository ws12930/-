-- ============================================================
-- Fenglin Blog —— 主库表结构
--
-- 用法：
--   CREATE DATABASE IF NOT EXISTS fenglin_blog
--     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   mysql -u root -p fenglin_blog < src/main/resources/db/schema.sql
--
-- 本脚本只负责建库后首次建表；字段扩展请用 schema_social.sql /
-- schema_notification.sql（幂等，可重复执行）。
-- ============================================================

SET NAMES utf8mb4;

-- ---------------------------- 用户 ----------------------------
CREATE TABLE IF NOT EXISTS blog_user (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username          VARCHAR(50)  NOT NULL                COMMENT '用户名，登录用',
    password_hash     VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密码摘要',
    nickname          VARCHAR(50)  NOT NULL                COMMENT '昵称',
    email             VARCHAR(100) NOT NULL                COMMENT '邮箱，找回密码用',
    avatar            VARCHAR(500)          DEFAULT NULL   COMMENT '头像：预设标识 ninja:0 或外链',
    signature         VARCHAR(140)          DEFAULT NULL   COMMENT '个性签名',
    faction           VARCHAR(16)           DEFAULT 'ninja' COMMENT '阵营 ninja / shinigami',
    role              VARCHAR(20)           DEFAULT 'USER' COMMENT '角色 USER / ADMIN',
    status            TINYINT              DEFAULT 1      COMMENT '1 正常 / 0 禁用',
    reset_token_hash  VARCHAR(100)          DEFAULT NULL   COMMENT '重置令牌摘要（SHA-256）',
    reset_token_expire DATETIME             DEFAULT NULL   COMMENT '重置令牌过期时间',
    created_at        DATETIME              DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_user_username (username),
    UNIQUE KEY uk_blog_user_email (email),
    KEY idx_blog_user_reset_token (reset_token_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户';

-- ---------------------------- 分类 ----------------------------
CREATE TABLE IF NOT EXISTS blog_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '分类名',
    slug        VARCHAR(80) NOT NULL COMMENT 'URL 标识',
    description VARCHAR(200)         DEFAULT NULL,
    sort_order  INT                  DEFAULT 0  COMMENT '排序值，越小越靠前',
    status      TINYINT              DEFAULT 1  COMMENT '1 启用 / 0 停用',
    created_at  DATETIME             DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_category_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章分类';

-- ---------------------------- 标签 ----------------------------
CREATE TABLE IF NOT EXISTS blog_tag (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50) NOT NULL,
    slug       VARCHAR(80) NOT NULL,
    created_at DATETIME             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_tag_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章标签';

-- ---------------------------- 文章 ----------------------------
CREATE TABLE IF NOT EXISTS blog_post (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    author_id    BIGINT       NOT NULL                COMMENT '作者 id',
    category_id  BIGINT                DEFAULT NULL   COMMENT '分类 id',
    title        VARCHAR(150) NOT NULL,
    slug         VARCHAR(120) NOT NULL                COMMENT 'URL 标识，全局唯一',
    summary      VARCHAR(500)          DEFAULT NULL,
    content      LONGTEXT     NOT NULL,
    cover_url    VARCHAR(500)          DEFAULT NULL,
    status       VARCHAR(16)           DEFAULT 'PUBLISHED' COMMENT 'DRAFT / PUBLISHED',
    view_count   BIGINT                DEFAULT 0,
    like_count   BIGINT                DEFAULT 0      COMMENT '冗余计数，以 blog_post_like 为准',
    pinned       TINYINT               DEFAULT 0      COMMENT '0 否 / 1 置顶',
    pinned_at    DATETIME              DEFAULT NULL,
    published_at DATETIME              DEFAULT NULL,
    created_at   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT               DEFAULT 0      COMMENT '逻辑删除 0 未删 / 1 已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_post_slug (slug),
    KEY idx_blog_post_author (author_id),
    KEY idx_blog_post_status_pinned (status, pinned, published_at),
    KEY idx_blog_post_category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章';

-- ---------------------------- 文章-标签关联 ----------------------------
CREATE TABLE IF NOT EXISTS blog_post_tag (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    post_id    BIGINT   NOT NULL,
    tag_id     BIGINT   NOT NULL,
    created_at DATETIME          DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_post_tag (post_id, tag_id),
    KEY idx_blog_post_tag_tag (tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章标签关联';

-- ---------------------------- 评论 ----------------------------
CREATE TABLE IF NOT EXISTS blog_comment (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    post_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    parent_id  BIGINT               DEFAULT NULL COMMENT '父评论 id，仅支持一层回复',
    content    VARCHAR(1000) NOT NULL,
    status     VARCHAR(16)          DEFAULT 'NORMAL',
    created_at DATETIME             DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT              DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_blog_comment_post (post_id, created_at),
    KEY idx_blog_comment_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论';

-- ---------------------------- 访问令牌 ----------------------------
CREATE TABLE IF NOT EXISTS blog_access_token (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(100) NOT NULL COMMENT '令牌摘要（SHA-256），明文不下发第二次',
    expire_at  DATETIME    NOT NULL,
    created_at DATETIME             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_access_token_hash (token_hash),
    KEY idx_blog_access_token_user (user_id),
    KEY idx_blog_access_token_expire (expire_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '访问令牌';

-- ---------------------------- 初始分类 ----------------------------
INSERT IGNORE INTO blog_category (id, name, slug, description, sort_order, status)
VALUES
    (1, '技术笔记', 'tech-notes', '写代码、踩坑与技术方案的记录', 1, 1),
    (2, '生活随笔', 'life-notes', '日常片段与偶尔冒出来的随想',   2, 1),
    (3, '读书观影', 'reviews',    '读过看过的内容与一点感想',     3, 1),
    (4, '闲聊讨论', 'chat',       '没什么主题的开放讨论',         4, 1);

-- ---------------------------- 初始标签 ----------------------------
INSERT IGNORE INTO blog_tag (id, name, slug)
VALUES
    (1, '前端',     'frontend'),
    (2, '后端',     'backend'),
    (3, '数据库',   'database'),
    (4, '效率工具', 'tools'),
    (5, '读书',     'book'),
    (6, '碎碎念',   'murmur');
