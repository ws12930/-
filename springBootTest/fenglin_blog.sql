/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : fenglin_blog

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 16/10/2026 15:06:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for blog_access_token
-- ----------------------------
DROP TABLE IF EXISTS `blog_access_token`;
CREATE TABLE `blog_access_token`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint UNSIGNED NOT NULL,
  `token_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SHA-256 access token hash',
  `expire_at` datetime(3) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_access_token_hash`(`token_hash` ASC) USING BTREE,
  INDEX `idx_blog_access_token_user_expire`(`user_id` ASC, `expire_at` ASC) USING BTREE,
  CONSTRAINT `fk_blog_access_token_user` FOREIGN KEY (`user_id`) REFERENCES `blog_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Login access tokens' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_access_token
-- ----------------------------
INSERT INTO `blog_access_token` VALUES (18, 3, '7930b8f9e79096551b53ff20f48e5d5cc935ad7b28d4afa2b8edf1709e33785f', '2026-11-08 14:18:21.926', '2026-10-09 14:18:21.926');
INSERT INTO `blog_access_token` VALUES (19, 6, '71d32d34d71cfb44ff2b279e243755ac8de4bdf50c5cd6d18d3c82c5ded16df4', '2026-10-09 16:26:23.774', '2026-10-09 14:26:23.774');
INSERT INTO `blog_access_token` VALUES (20, 6, '9dd1b03d6bc2605b581d2bb30e385417dc70f8008594f2de53bdcea5f7b8981b', '2026-10-09 16:26:30.086', '2026-10-09 14:26:30.086');
INSERT INTO `blog_access_token` VALUES (21, 6, '996ec5cd1402097345ed2b9edf0f14903e6aaf4a47f565cd2538896675e611fb', '2026-10-09 16:26:42.480', '2026-10-09 14:26:42.480');
INSERT INTO `blog_access_token` VALUES (22, 6, 'ff2ac6eb4678c4d49f825d3cfd0047ef75b37d4cbcc6f327e365e190a74fb01c', '2026-10-09 16:27:36.348', '2026-10-09 14:27:36.348');
INSERT INTO `blog_access_token` VALUES (23, 7, 'a4bc6cb3c0b17a50740d254b4c033390ccb3d2acfa529ac154b14d66dd2a55fc', '2026-10-09 16:27:36.574', '2026-10-09 14:27:36.574');
INSERT INTO `blog_access_token` VALUES (24, 3, '800986384af58f1e7bcd684e61589ef355133b50d4fae3daf547cdf297f05946', '2026-11-15 13:38:05.668', '2026-10-16 13:38:05.668');
INSERT INTO `blog_access_token` VALUES (25, 3, '5a514e8ff27a44e96885618a03cc23db210b89cf5d3e88bd577cc5996af386e5', '2026-11-15 13:39:17.828', '2026-10-16 13:39:17.828');
INSERT INTO `blog_access_token` VALUES (26, 2, '796da71a17375643bcbe87947e57db540aa963012de84633ca09a3ecda3ee636', '2026-11-15 13:40:13.684', '2026-10-16 13:40:13.685');
INSERT INTO `blog_access_token` VALUES (28, 2, '70805b71caa3af5ac014c5527114463641cfd3bd86497246bd0ddbf2b6e20af6', '2026-11-15 13:53:29.526', '2026-10-16 13:53:29.526');
INSERT INTO `blog_access_token` VALUES (29, 3, 'b5275ae165bb251558abca60e96f73490c74be16f82ae81ca8266b461e1c5936', '2026-11-15 13:53:38.187', '2026-10-16 13:53:38.187');
INSERT INTO `blog_access_token` VALUES (30, 2, '54c002d5bf8915108ad7ddcc3ee40884a9f3899ca3c474950c593744fec3c924', '2026-10-16 16:11:04.054', '2026-10-16 14:11:04.054');

-- ----------------------------
-- Table structure for blog_category
-- ----------------------------
DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `sort_order` int NOT NULL DEFAULT 0,
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_category_name`(`name` ASC) USING BTREE,
  UNIQUE INDEX `uk_blog_category_slug`(`slug` ASC) USING BTREE,
  INDEX `idx_blog_category_status_sort`(`status` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Article categories' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_category
-- ----------------------------
INSERT INTO `blog_category` VALUES (1, '默认分类', 'default', '尚未归入其他分类的文章', 0, 1, '2026-10-08 13:51:29.441', '2026-10-08 13:51:29.441');
INSERT INTO `blog_category` VALUES (2, '死火对决', 'bleach-vs-naruto', 'BLEACH vs NARUTO 主题分类', 1, 1, '2026-10-08 13:56:14.482', '2026-10-08 13:56:14.482');

-- ----------------------------
-- Table structure for blog_comment
-- ----------------------------
DROP TABLE IF EXISTS `blog_comment`;
CREATE TABLE `blog_comment`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id` bigint UNSIGNED NOT NULL,
  `user_id` bigint UNSIGNED NOT NULL,
  `parent_id` bigint UNSIGNED NULL DEFAULT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING, APPROVED or REJECTED',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_blog_comment_post_status_created`(`post_id` ASC, `status` ASC, `deleted` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_blog_comment_user_created`(`user_id` ASC, `deleted` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_blog_comment_parent`(`parent_id` ASC) USING BTREE,
  CONSTRAINT `fk_blog_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `blog_comment` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_blog_comment_post` FOREIGN KEY (`post_id`) REFERENCES `blog_post` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_blog_comment_user` FOREIGN KEY (`user_id`) REFERENCES `blog_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Article comments' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_comment
-- ----------------------------
INSERT INTO `blog_comment` VALUES (2, 3, 3, NULL, '一护，来战！', 'APPROVED', '2026-10-08 14:41:31.120', '2026-10-08 14:41:31.120', 0);
INSERT INTO `blog_comment` VALUES (3, 3, 2, 2, '随时奉陪，鸣人。', 'APPROVED', '2026-10-08 14:41:31.463', '2026-10-08 14:41:31.463', 0);
INSERT INTO `blog_comment` VALUES (4, 2, 2, NULL, '根本没有这种随笔', 'APPROVED', '2026-10-08 14:45:48.958', '2026-10-08 14:45:48.958', 0);
INSERT INTO `blog_comment` VALUES (5, 2, 2, NULL, '你只是怕了', 'APPROVED', '2026-10-08 14:45:59.002', '2026-10-08 14:51:36.315', 1);
INSERT INTO `blog_comment` VALUES (6, 3, 2, 2, '牛魔酬宾', 'APPROVED', '2026-10-08 14:50:59.341', '2026-10-08 14:51:12.547', 1);
INSERT INTO `blog_comment` VALUES (7, 2, 2, 4, '你只是怕了', 'APPROVED', '2026-10-08 14:51:33.638', '2026-10-08 14:51:33.638', 0);
INSERT INTO `blog_comment` VALUES (8, 2, 2, 4, '这他妈谁不怕', 'APPROVED', '2026-10-08 14:51:46.402', '2026-10-08 14:51:46.402', 0);
INSERT INTO `blog_comment` VALUES (9, 3, 2, NULL, '前端路径回复测试', 'APPROVED', '2026-10-08 14:52:04.876', '2026-10-08 14:52:04.876', 0);
INSERT INTO `blog_comment` VALUES (10, 3, 3, 9, '收到！', 'APPROVED', '2026-10-08 14:52:05.286', '2026-10-08 14:52:05.286', 0);
INSERT INTO `blog_comment` VALUES (11, 6, 2, NULL, '你发牛魔酬宾😡', 'APPROVED', '2026-10-09 14:16:14.066', '2026-10-09 14:16:14.066', 0);
INSERT INTO `blog_comment` VALUES (13, 6, 3, 11, '狂你个雷咪🤣', 'APPROVED', '2026-10-09 14:19:05.157', '2026-10-09 14:19:05.157', 0);
INSERT INTO `blog_comment` VALUES (14, 6, 3, NULL, '[📎 AI项目生成步骤.txt](/uploads/2026/10/f948d5f757434b5a834fdfed6ce133b1.txt)', 'APPROVED', '2026-10-09 14:26:28.789', '2026-10-09 14:26:28.789', 0);
INSERT INTO `blog_comment` VALUES (15, 8, 6, NULL, '评论里的图：![宽图](/uploads/2026/10/a4082ce2117d42639ea9f515202a87b7.png)', 'APPROVED', '2026-10-09 14:26:42.538', '2026-10-09 14:26:42.538', 0);

-- ----------------------------
-- Table structure for blog_follow
-- ----------------------------
DROP TABLE IF EXISTS `blog_follow`;
CREATE TABLE `blog_follow`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `follower_id` bigint UNSIGNED NOT NULL COMMENT '关注者',
  `followee_id` bigint UNSIGNED NOT NULL COMMENT '被关注者',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_follow_pair`(`follower_id` ASC, `followee_id` ASC) USING BTREE,
  INDEX `idx_blog_follow_followee`(`followee_id` ASC) USING BTREE,
  CONSTRAINT `fk_blog_follow_followee` FOREIGN KEY (`followee_id`) REFERENCES `blog_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_blog_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `blog_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'User follow relations' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_follow
-- ----------------------------
INSERT INTO `blog_follow` VALUES (6, 3, 2, '2026-10-16 14:00:28.781');
INSERT INTO `blog_follow` VALUES (7, 2, 3, '2026-10-16 15:03:05.447');

-- ----------------------------
-- Table structure for blog_message
-- ----------------------------
DROP TABLE IF EXISTS `blog_message`;
CREATE TABLE `blog_message`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `sender_id` bigint UNSIGNED NOT NULL,
  `receiver_id` bigint UNSIGNED NOT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `read_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1 已读',
  `sender_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `receiver_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_blog_msg_receiver`(`receiver_id` ASC, `read_flag` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_blog_msg_pair`(`sender_id` ASC, `receiver_id` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_blog_msg_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `blog_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_blog_msg_sender` FOREIGN KEY (`sender_id`) REFERENCES `blog_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Private messages' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_message
-- ----------------------------
INSERT INTO `blog_message` VALUES (1, 2, 3, '明天现世见，别迟到', 1, 0, 0, '2026-10-08 14:41:31.988');
INSERT INTO `blog_message` VALUES (2, 2, 3, '你哪位', 1, 0, 0, '2026-10-08 14:55:30.528');
INSERT INTO `blog_message` VALUES (3, 2, 3, '卡顿', 1, 0, 0, '2026-10-08 14:55:46.688');
INSERT INTO `blog_message` VALUES (4, 3, 2, '欧坤', 1, 0, 0, '2026-10-09 13:44:40.783');
INSERT INTO `blog_message` VALUES (5, 3, 2, '1', 1, 0, 0, '2026-10-09 13:47:18.450');
INSERT INTO `blog_message` VALUES (6, 3, 2, '1', 1, 0, 0, '2026-10-09 13:47:18.933');
INSERT INTO `blog_message` VALUES (7, 3, 2, '1', 1, 0, 0, '2026-10-09 13:47:19.384');
INSERT INTO `blog_message` VALUES (8, 3, 2, '1', 1, 0, 0, '2026-10-09 13:47:19.831');
INSERT INTO `blog_message` VALUES (9, 3, 2, '1', 1, 0, 0, '2026-10-09 13:47:21.596');
INSERT INTO `blog_message` VALUES (25, 3, 2, '乐子', 1, 0, 0, '2026-10-09 14:08:53.798');
INSERT INTO `blog_message` VALUES (26, 2, 3, '😭', 1, 0, 0, '2026-10-09 14:15:53.225');
INSERT INTO `blog_message` VALUES (28, 3, 2, '⚡', 1, 0, 0, '2026-10-09 14:18:29.830');
INSERT INTO `blog_message` VALUES (29, 6, 7, '这条是普通文字消息，用来看气泡基准宽度。', 0, 0, 0, '2026-10-09 14:27:36.607');
INSERT INTO `blog_message` VALUES (30, 6, 7, '带图消息：\n![宽图](/uploads/2026/10/45ccd4c823a84d8383392d6205586666.png)', 0, 0, 0, '2026-10-09 14:27:36.618');
INSERT INTO `blog_message` VALUES (31, 3, 2, '😴', 1, 0, 0, '2026-10-09 15:06:51.041');
INSERT INTO `blog_message` VALUES (32, 3, 2, '😱😱😱😱', 1, 0, 0, '2026-10-09 15:06:55.431');
INSERT INTO `blog_message` VALUES (33, 3, 2, '👏', 1, 0, 0, '2026-10-16 14:00:11.419');

-- ----------------------------
-- Table structure for blog_post
-- ----------------------------
DROP TABLE IF EXISTS `blog_post`;
CREATE TABLE `blog_post`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `author_id` bigint UNSIGNED NOT NULL,
  `category_id` bigint UNSIGNED NOT NULL,
  `title` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT or PUBLISHED',
  `view_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `like_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Denormalized like count',
  `published_at` datetime(3) NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_post_slug`(`slug` ASC) USING BTREE,
  INDEX `idx_blog_post_status_publish`(`status` ASC, `deleted` ASC, `published_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_blog_post_category_status_publish`(`category_id` ASC, `status` ASC, `deleted` ASC, `published_at` ASC) USING BTREE,
  INDEX `idx_blog_post_author_updated`(`author_id` ASC, `deleted` ASC, `updated_at` ASC) USING BTREE,
  CONSTRAINT `fk_blog_post_author` FOREIGN KEY (`author_id`) REFERENCES `blog_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_blog_post_category` FOREIGN KEY (`category_id`) REFERENCES `blog_category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Blog articles' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_post
-- ----------------------------
INSERT INTO `blog_post` VALUES (2, 2, 2, '原神与鸣潮对决', 'post-693ca230607e', '两个乐子群体争斗', '我是棍木', '', 'PUBLISHED', 9, 1, '2026-10-08 14:11:48.614', '2026-10-08 14:11:48.614', '2026-10-16 15:05:13.104', 0);
INSERT INTO `blog_post` VALUES (3, 2, 1, 'BLEACH × NARUTO 开服公告', 'bleach-naruto-open', '双阵营对决正式开启，选择你的阵营', '木叶与尸魂界的战场已经铺开，忍者与死神在此交汇。', '', 'PUBLISHED', 6, 2, '2026-10-08 14:20:16.250', '2026-10-08 14:20:16.250', '2026-10-09 13:40:50.922', 0);
INSERT INTO `blog_post` VALUES (4, 2, 1, '斩魄刀 vs 尾兽：战力体系对比', 'zanpakuto-vs-tailed-beast', '两种力量体系的设定差异', '始解、卍解与尾兽化、仙人模式，哪一套更强？', '', 'PUBLISHED', 2, 1, '2026-10-07 14:20:22.000', '2026-10-07 14:20:22.000', '2026-10-08 14:54:47.418', 0);
INSERT INTO `blog_post` VALUES (5, 2, 1, '木叶与尸魂界：世界观随笔', 'world-building-note', '从忍者村到瀞灵廷', '两个世界的组织架构与权力运转方式对比。', '', 'PUBLISHED', 1, 1, '2026-10-05 14:20:22.000', '2026-10-05 14:20:22.000', '2026-10-08 14:22:42.001', 0);
INSERT INTO `blog_post` VALUES (6, 3, 1, '棍木', 'post-23720e56d7f7', '棍木', '棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木棍木', '', 'PUBLISHED', 13, 1, '2026-10-09 13:54:01.085', '2026-10-09 13:54:01.085', '2026-10-16 15:00:59.186', 0);
INSERT INTO `blog_post` VALUES (8, 6, 1, '图片尺寸验证', 'post-ddf4b6b73532', '这是一篇用于验证图片尺寸的测试文章。 ![宽图](/uploads/2026/10/a4082ce2117d42639ea9f515202a87b7.png) 上面是宽图（1600×400），下面是长图（600×1400）。 ![长图](/uploads/2026/10/91e4d74a2aca4555936105d7', '这是一篇用于验证图片尺寸的测试文章。\n\n![宽图](/uploads/2026/10/a4082ce2117d42639ea9f515202a87b7.png)\n\n上面是宽图（1600×400），下面是长图（600×1400）。\n\n![长图](/uploads/2026/10/91e4d74a2aca4555936105d7356c0a55.png)\n\n正文结束。', '', 'PUBLISHED', 10, 0, '2026-10-09 14:26:42.525', '2026-10-09 14:26:42.525', '2026-10-16 14:58:13.060', 0);

-- ----------------------------
-- Table structure for blog_post_like
-- ----------------------------
DROP TABLE IF EXISTS `blog_post_like`;
CREATE TABLE `blog_post_like`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id` bigint UNSIGNED NOT NULL,
  `user_id` bigint UNSIGNED NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_like_post_user`(`post_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_blog_like_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_blog_like_post` FOREIGN KEY (`post_id`) REFERENCES `blog_post` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_blog_like_user` FOREIGN KEY (`user_id`) REFERENCES `blog_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Post likes' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_post_like
-- ----------------------------
INSERT INTO `blog_post_like` VALUES (19, 5, 2, '2026-10-08 14:48:08.557');
INSERT INTO `blog_post_like` VALUES (20, 3, 3, '2026-10-08 14:52:04.459');
INSERT INTO `blog_post_like` VALUES (21, 2, 2, '2026-10-08 14:54:33.481');
INSERT INTO `blog_post_like` VALUES (24, 3, 2, '2026-10-08 14:54:42.903');
INSERT INTO `blog_post_like` VALUES (25, 4, 2, '2026-10-08 14:54:46.432');
INSERT INTO `blog_post_like` VALUES (35, 6, 3, '2026-10-09 14:27:08.777');

-- ----------------------------
-- Table structure for blog_post_tag
-- ----------------------------
DROP TABLE IF EXISTS `blog_post_tag`;
CREATE TABLE `blog_post_tag`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id` bigint UNSIGNED NOT NULL,
  `tag_id` bigint UNSIGNED NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_post_tag`(`post_id` ASC, `tag_id` ASC) USING BTREE,
  INDEX `idx_blog_post_tag_tag_post`(`tag_id` ASC, `post_id` ASC) USING BTREE,
  CONSTRAINT `fk_blog_post_tag_post` FOREIGN KEY (`post_id`) REFERENCES `blog_post` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_blog_post_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `blog_tag` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Article and tag relation' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_post_tag
-- ----------------------------
INSERT INTO `blog_post_tag` VALUES (1, 2, 3, '2026-10-08 14:11:48.631');
INSERT INTO `blog_post_tag` VALUES (2, 6, 1, '2026-10-09 13:54:01.098');
INSERT INTO `blog_post_tag` VALUES (3, 6, 3, '2026-10-09 13:54:01.098');

-- ----------------------------
-- Table structure for blog_tag
-- ----------------------------
DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_tag_name`(`name` ASC) USING BTREE,
  UNIQUE INDEX `uk_blog_tag_slug`(`slug` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Article tags' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_tag
-- ----------------------------
INSERT INTO `blog_tag` VALUES (1, 'Java', 'java', '2026-10-08 13:51:29.450');
INSERT INTO `blog_tag` VALUES (2, 'Spring Boot', 'spring-boot', '2026-10-08 13:51:29.450');
INSERT INTO `blog_tag` VALUES (3, '随笔', 'notes', '2026-10-08 13:51:29.450');

-- ----------------------------
-- Table structure for blog_user
-- ----------------------------
DROP TABLE IF EXISTS `blog_user`;
CREATE TABLE `blog_user`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Unique login name',
  `password_hash` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt password hash',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Display name',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Unique email',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'Avatar URL',
  `signature` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'Personal signature',
  `faction` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ninja' COMMENT 'ninja or shinigami',
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER' COMMENT 'USER or ADMIN',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '1 active, 0 disabled',
  `reset_token_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'SHA-256 password reset token hash',
  `reset_token_expire` datetime(3) NULL DEFAULT NULL COMMENT 'Password reset token expiration',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_user_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_blog_user_email`(`email` ASC) USING BTREE,
  INDEX `idx_blog_user_reset_token`(`reset_token_hash` ASC) USING BTREE,
  INDEX `idx_blog_user_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Blog users' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_user
-- ----------------------------
INSERT INTO `blog_user` VALUES (2, 'admin', '$2a$10$tAbpj62ed0NahhirKaBQDOXC5yZX/htSV6StsIoYVj9jQ7668TH9S', '野狗', 'admin@example.com', 'ninja:2', '狗野', 'ninja', 'USER', 1, NULL, NULL, '2026-10-08 14:08:51.490', '2026-10-16 13:40:00.458');
INSERT INTO `blog_user` VALUES (3, 'naruto01', '$2a$10$z5Zwjq14KVAik72GqYk9TOvK3sKzJlqpR1yNvTHrMeKnQx8cbduIO', '漩涡老孙', 'naruto01@example.com', 'ninja:0', '', 'ninja', 'USER', 1, NULL, NULL, '2026-10-08 14:41:29.385', '2026-10-09 14:18:14.070');
INSERT INTO `blog_user` VALUES (6, 'uitest_img', '$2a$10$Vk3kAUjUbb.lixhxBQBttesRjT.bK4NHP1T69p/MHWgBPDk3qSoe2', '图片测试', 'uitestimg@t.com', '', '', 'ninja', 'USER', 1, NULL, NULL, '2026-10-09 14:26:23.576', '2026-10-09 14:26:23.576');
INSERT INTO `blog_user` VALUES (7, 'uitest_img2', '$2a$10$IJOtSyYwunEy7AjHkCimee4/npwVfsv1XKdOYMHD46FdhgO3eKH3e', '图片测试B', 'uitestimg2@t.com', '', '', 'ninja', 'USER', 1, NULL, NULL, '2026-10-09 14:27:36.369', '2026-10-09 14:27:36.369');

SET FOREIGN_KEY_CHECKS = 1;
