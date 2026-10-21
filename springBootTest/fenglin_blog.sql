/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : fenglin_blog

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 21/10/2026 16:51:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for blog_access_token
-- ----------------------------
DROP TABLE IF EXISTS `blog_access_token`;
CREATE TABLE `blog_access_token`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token_hash` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '令牌摘要（SHA-256），明文不下发第二次',
  `expire_at` datetime NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_access_token_hash`(`token_hash` ASC) USING BTREE,
  INDEX `idx_blog_access_token_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_blog_access_token_expire`(`expire_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '访问令牌' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_access_token
-- ----------------------------
INSERT INTO `blog_access_token` VALUES (1, 1, '442698b5e30072f037360982b43f66d45359601fc36c7e6fb9053ff36892b95f', '2026-10-28 16:08:12', '2026-10-21 16:08:12');
INSERT INTO `blog_access_token` VALUES (2, 2, '40c2c611186a62b4f159b9d29be6c55fc4cbfad79eb0fe3868aec41c5e6cecfc', '2026-10-28 16:08:12', '2026-10-21 16:08:12');
INSERT INTO `blog_access_token` VALUES (3, 1, '529c558167e8cae660de02e3616d12df219f0c71765ebea60ddd2f1f0f8289f4', '2026-10-28 16:14:26', '2026-10-21 16:14:26');
INSERT INTO `blog_access_token` VALUES (4, 2, '7b66fe072ecdd667c9df2897be7770da6f80eb6d164258a4608daba49a79d45c', '2026-10-28 16:14:26', '2026-10-21 16:14:26');
INSERT INTO `blog_access_token` VALUES (5, 1, '596784aa00e01fd418241b218a46d5d7208b1885bdaede2dd5909f167612ac3e', '2026-11-20 16:17:31', '2026-10-21 16:17:31');
INSERT INTO `blog_access_token` VALUES (6, 2, 'bcae4b90ac4b4a8bcac30d1af8829fd60e8eee351affa664dc1532d89f2d2410', '2026-10-28 16:18:40', '2026-10-21 16:18:40');
INSERT INTO `blog_access_token` VALUES (7, 1, '4fb9bf63c2bf742034768494a1578132b42256e3ae25e9d5ab2ab7ebec83dec1', '2026-10-28 16:28:18', '2026-10-21 16:28:18');
INSERT INTO `blog_access_token` VALUES (8, 2, 'c95a6436851649f20975f2eaca63a10ecc74aaee7da791863b0937b4a6d7d2a5', '2026-10-28 16:28:18', '2026-10-21 16:28:18');
INSERT INTO `blog_access_token` VALUES (9, 1, '04b21e75a09afa459ca4e202913f252fcdaa159235525938c8f043cffd30240e', '2026-10-28 16:28:52', '2026-10-21 16:28:52');
INSERT INTO `blog_access_token` VALUES (10, 2, 'b948e39e107e6b4a316db167197063d437449ffd182fc3e2663f0a8e04c044f7', '2026-10-28 16:28:52', '2026-10-21 16:28:52');

-- ----------------------------
-- Table structure for blog_category
-- ----------------------------
DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名',
  `slug` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'URL 标识',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `sort_order` int NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status` tinyint NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_category_slug`(`slug` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章分类' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_category
-- ----------------------------
INSERT INTO `blog_category` VALUES (1, '技术笔记', 'tech-notes', '写代码、踩坑与技术方案的记录', 1, 1, '2026-10-21 15:56:54', '2026-10-21 16:45:35');
INSERT INTO `blog_category` VALUES (2, '生活随笔', 'life-notes', '日常片段与偶尔冒出来的随想', 2, 1, '2026-10-21 15:56:54', '2026-10-21 16:45:35');
INSERT INTO `blog_category` VALUES (3, '读书观影', 'reviews', '读过看过的内容与一点感想', 3, 1, '2026-10-21 15:56:54', '2026-10-21 16:45:35');
INSERT INTO `blog_category` VALUES (4, '闲聊讨论', 'chat', '没什么主题的开放讨论', 4, 1, '2026-10-21 15:56:54', '2026-10-21 16:45:35');

-- ----------------------------
-- Table structure for blog_comment
-- ----------------------------
DROP TABLE IF EXISTS `blog_comment`;
CREATE TABLE `blog_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父评论 id，仅支持一层回复',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'NORMAL',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_blog_comment_post`(`post_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_blog_comment_parent`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_comment
-- ----------------------------

-- ----------------------------
-- Table structure for blog_follow
-- ----------------------------
DROP TABLE IF EXISTS `blog_follow`;
CREATE TABLE `blog_follow`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `follower_id` bigint NOT NULL COMMENT '关注者（粉丝）',
  `followee_id` bigint NOT NULL COMMENT '被关注者',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_follow`(`follower_id` ASC, `followee_id` ASC) USING BTREE,
  INDEX `idx_blog_follow_followee`(`followee_id` ASC) USING BTREE,
  INDEX `idx_blog_follow_follower`(`follower_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '关注关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_follow
-- ----------------------------
INSERT INTO `blog_follow` VALUES (4, 2, 1, '2026-10-21 16:28:52');
INSERT INTO `blog_follow` VALUES (6, 1, 2, '2026-10-21 16:46:16');

-- ----------------------------
-- Table structure for blog_message
-- ----------------------------
DROP TABLE IF EXISTS `blog_message`;
CREATE TABLE `blog_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL,
  `receiver_id` bigint NOT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `read_flag` tinyint NULL DEFAULT 0 COMMENT '0 未读 / 1 已读',
  `sender_deleted` tinyint NULL DEFAULT 0 COMMENT '发送方是否删除会话',
  `receiver_deleted` tinyint NULL DEFAULT 0 COMMENT '接收方是否删除会话',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_blog_message_conversation`(`sender_id` ASC, `receiver_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_blog_message_unread`(`receiver_id` ASC, `read_flag` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私信' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_message
-- ----------------------------
INSERT INTO `blog_message` VALUES (1, 2, 1, 'hello', 1, 0, 0, '2026-10-21 16:30:42');
INSERT INTO `blog_message` VALUES (2, 2, 1, '😍', 1, 0, 0, '2026-10-21 16:30:51');
INSERT INTO `blog_message` VALUES (3, 1, 2, '老弟的乐子', 1, 0, 0, '2026-10-21 16:33:31');

-- ----------------------------
-- Table structure for blog_notification
-- ----------------------------
DROP TABLE IF EXISTS `blog_notification`;
CREATE TABLE `blog_notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '通知接收者',
  `actor_id` bigint NOT NULL COMMENT '触发者',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'LIKE / COMMENT / REPLY / FOLLOW',
  `post_id` bigint NULL DEFAULT NULL,
  `comment_id` bigint NULL DEFAULT NULL,
  `content` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '摘要文本（评论内容预览等）',
  `read_flag` tinyint NULL DEFAULT 0 COMMENT '0 未读 / 1 已读',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_blog_notification_user`(`user_id` ASC, `read_flag` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_blog_notification_actor`(`actor_id` ASC, `type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '站内通知' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_notification
-- ----------------------------
INSERT INTO `blog_notification` VALUES (4, 1, 2, 'FOLLOW', NULL, NULL, NULL, 1, '2026-10-21 16:28:52');
INSERT INTO `blog_notification` VALUES (6, 2, 1, 'FOLLOW', NULL, NULL, NULL, 0, '2026-10-21 16:46:16');

-- ----------------------------
-- Table structure for blog_post
-- ----------------------------
DROP TABLE IF EXISTS `blog_post`;
CREATE TABLE `blog_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author_id` bigint NOT NULL COMMENT '作者 id',
  `category_id` bigint NULL DEFAULT NULL COMMENT '分类 id',
  `title` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'URL 标识，全局唯一',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT / PUBLISHED',
  `view_count` bigint NULL DEFAULT 0,
  `like_count` bigint NULL DEFAULT 0 COMMENT '冗余计数，以 blog_post_like 为准',
  `pinned` tinyint NULL DEFAULT 0 COMMENT '0 否 / 1 置顶',
  `pinned_at` datetime NULL DEFAULT NULL,
  `published_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 / 1 已删',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_post_slug`(`slug` ASC) USING BTREE,
  INDEX `idx_blog_post_author`(`author_id` ASC) USING BTREE,
  INDEX `idx_blog_post_status_pinned`(`status` ASC, `pinned` ASC, `published_at` ASC) USING BTREE,
  INDEX `idx_blog_post_category`(`category_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_post
-- ----------------------------

-- ----------------------------
-- Table structure for blog_post_like
-- ----------------------------
DROP TABLE IF EXISTS `blog_post_like`;
CREATE TABLE `blog_post_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_post_like`(`post_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_blog_post_like_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章点赞' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_post_like
-- ----------------------------

-- ----------------------------
-- Table structure for blog_post_tag
-- ----------------------------
DROP TABLE IF EXISTS `blog_post_tag`;
CREATE TABLE `blog_post_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_post_tag`(`post_id` ASC, `tag_id` ASC) USING BTREE,
  INDEX `idx_blog_post_tag_tag`(`tag_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章标签关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_post_tag
-- ----------------------------

-- ----------------------------
-- Table structure for blog_tag
-- ----------------------------
DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_tag_slug`(`slug` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章标签' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_tag
-- ----------------------------
INSERT INTO `blog_tag` VALUES (1, '前端', 'frontend', '2026-10-21 15:56:54');
INSERT INTO `blog_tag` VALUES (2, '后端', 'backend', '2026-10-21 15:56:54');
INSERT INTO `blog_tag` VALUES (3, '数据库', 'database', '2026-10-21 15:56:54');
INSERT INTO `blog_tag` VALUES (4, '效率工具', 'tools', '2026-10-21 15:56:54');
INSERT INTO `blog_tag` VALUES (5, '读书', 'book', '2026-10-21 15:56:54');
INSERT INTO `blog_tag` VALUES (6, '碎碎念', 'murmur', '2026-10-21 15:56:54');

-- ----------------------------
-- Table structure for blog_user
-- ----------------------------
DROP TABLE IF EXISTS `blog_user`;
CREATE TABLE `blog_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名，登录用',
  `password_hash` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt 密码摘要',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '昵称',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱，找回密码用',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像：预设标识 ninja:0 或外链',
  `signature` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '个性签名',
  `faction` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ninja' COMMENT '阵营 ninja / shinigami',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'USER' COMMENT '角色 USER / ADMIN',
  `status` tinyint NULL DEFAULT 1 COMMENT '1 正常 / 0 禁用',
  `reset_token_hash` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '重置令牌摘要（SHA-256）',
  `reset_token_expire` datetime NULL DEFAULT NULL COMMENT '重置令牌过期时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_user_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_blog_user_email`(`email` ASC) USING BTREE,
  INDEX `idx_blog_user_reset_token`(`reset_token_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog_user
-- ----------------------------
INSERT INTO `blog_user` VALUES (1, 'qinghe', '$2a$10$grTvthiFX81jmF3M.8VjXeD2rIAhULIzO9pHleDfBGveDgEYKHOqO', '青禾', 'qinghe@fenglin.blog', 'ninja:0', '', 'ninja', 'USER', 1, NULL, NULL, '2026-10-21 16:08:12', '2026-10-21 16:08:12');
INSERT INTO `blog_user` VALUES (2, 'jingling', '$2a$10$YgoogTThK3yCawTa8B50/eOAZDjrVLI8Tm8aJ.IBrFGdQ.rxmflsC', '景翎', 'jingling@fenglin.blog', 'ninja:0', '', 'ninja', 'USER', 1, NULL, NULL, '2026-10-21 16:08:12', '2026-10-21 16:08:12');

SET FOREIGN_KEY_CHECKS = 1;
