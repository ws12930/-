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

 Date: 30/10/2026 15:15:22
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
) ENGINE = InnoDB AUTO_INCREMENT = 53 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '访问令牌' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_access_token
-- ----------------------------
INSERT INTO `blog_access_token` VALUES (1, 1, '442698b5e30072f037360982b43f66d45359601fc36c7e6fb9053ff36892b95f', '2026-10-28 16:08:12', '2026-10-21 16:08:12');
INSERT INTO `blog_access_token` VALUES (3, 1, '529c558167e8cae660de02e3616d12df219f0c71765ebea60ddd2f1f0f8289f4', '2026-10-28 16:14:26', '2026-10-21 16:14:26');
INSERT INTO `blog_access_token` VALUES (5, 1, '596784aa00e01fd418241b218a46d5d7208b1885bdaede2dd5909f167612ac3e', '2026-11-20 16:17:31', '2026-10-21 16:17:31');
INSERT INTO `blog_access_token` VALUES (7, 1, '4fb9bf63c2bf742034768494a1578132b42256e3ae25e9d5ab2ab7ebec83dec1', '2026-10-28 16:28:18', '2026-10-21 16:28:18');
INSERT INTO `blog_access_token` VALUES (9, 1, '04b21e75a09afa459ca4e202913f252fcdaa159235525938c8f043cffd30240e', '2026-10-28 16:28:52', '2026-10-21 16:28:52');
INSERT INTO `blog_access_token` VALUES (11, 1, 'ee3b987ad00247e91147d84fb3567aa9c7688ad6a1722da407eabc140ea4a261', '2026-10-29 13:38:05', '2026-10-22 13:38:05');
INSERT INTO `blog_access_token` VALUES (12, 1, '6e2c908197fa5c083bda3ae08b6d1ddd0829bd584a61e79327da862cdbb30a95', '2026-10-29 13:38:58', '2026-10-22 13:38:58');
INSERT INTO `blog_access_token` VALUES (14, 1, '6f250a9cefd0f88bb5108a41ac16d3a3292122e368b2fd9cc020e90d83e2702e', '2026-10-29 13:40:27', '2026-10-22 13:40:27');
INSERT INTO `blog_access_token` VALUES (15, 1, 'f978fe3de55aa535e1d755b772b525f666ababb5729e92f23ec62fbb87a9b681', '2026-10-29 14:09:39', '2026-10-22 14:09:39');
INSERT INTO `blog_access_token` VALUES (16, 1, 'a99981ba47c1a0c3b40a55541ab1e6e61eac3138b8b2cafa55e41495848bd39b', '2026-10-29 14:10:05', '2026-10-22 14:10:05');
INSERT INTO `blog_access_token` VALUES (17, 1, 'b3646470c4704581b49b54a3bf816ece3868a1daf29d31c792e3579ebf115256', '2026-10-29 14:10:16', '2026-10-22 14:10:16');
INSERT INTO `blog_access_token` VALUES (18, 1, '5edde39c435b832c71580225ffecb9afaf9bf95acc87a93879ea3b0a37791ac4', '2026-10-29 14:20:39', '2026-10-22 14:20:39');
INSERT INTO `blog_access_token` VALUES (19, 1, 'b3774ade8103f7691d6a954ccf0b34b5afd691cf99fdaf9b0969db15e1f58cf9', '2026-10-29 14:20:54', '2026-10-22 14:20:54');
INSERT INTO `blog_access_token` VALUES (20, 1, '349e3652beb795300ce6dd7ce29bd7d19b8eda93b5898bd4aa450ceee5961fa3', '2026-11-02 08:40:42', '2026-10-26 08:40:42');
INSERT INTO `blog_access_token` VALUES (21, 1, '58c73057e4c95327859c8a17b73f2c3dc06550caae2e0ae41b37ca59d1483fc8', '2026-11-02 08:53:34', '2026-10-26 08:53:34');
INSERT INTO `blog_access_token` VALUES (23, 1, '86a1a8efa57ce775535cf9ecef2fc0880d3d24a8ef6ff88ba4f2699989460d07', '2026-11-02 09:09:15', '2026-10-26 09:09:15');
INSERT INTO `blog_access_token` VALUES (25, 1, 'c68cb50d01176cea393e3a349709a4a150875050a9f787c3ed3af5ff4cd8186c', '2026-11-02 09:09:34', '2026-10-26 09:09:34');
INSERT INTO `blog_access_token` VALUES (26, 1, 'f46e69ac11c72635fee3dd48fba73fc038b098ddad4611293eb6f6fe640686fa', '2026-11-02 09:15:38', '2026-10-26 09:15:38');
INSERT INTO `blog_access_token` VALUES (27, 1, '727cf458efc349e46ccf3122d6d20f5f8e9ef0599c057493257f6f0d370429f7', '2026-11-02 09:22:52', '2026-10-26 09:22:52');
INSERT INTO `blog_access_token` VALUES (28, 1, '02f984be5161dfd48650452d6166061fb5357daa094795c1f2df05d2d810af4a', '2026-11-02 09:36:10', '2026-10-26 09:36:10');
INSERT INTO `blog_access_token` VALUES (30, 1, '80bac91ed77fa708d7166e4fe856b86f87e0b26e318d22ce900b1c4b8fd85b66', '2026-11-02 09:38:04', '2026-10-26 09:38:04');
INSERT INTO `blog_access_token` VALUES (31, 1, 'c72c589f3c4c817cdc04288b46cae4d4c8d2e3632b86271ecd5cb56359f7a2cb', '2026-11-02 09:38:29', '2026-10-26 09:38:29');
INSERT INTO `blog_access_token` VALUES (32, 1, 'ca0935e691962b1af54498c11e05a684c853d94fc8b4e18034604d394056f4bf', '2026-11-06 14:09:15', '2026-10-30 14:09:15');
INSERT INTO `blog_access_token` VALUES (33, 1, 'd1ec4f9924e5033f88e6a0330e2055d90c3deeba14df32ab082f66c597469b17', '2026-11-29 14:10:04', '2026-10-30 14:10:04');
INSERT INTO `blog_access_token` VALUES (35, 1, 'dd68c507cd1e5ae9fd4c8c1723e17d9ec7fd202c70da3bb294c7bf0c0cb7adca', '2026-11-06 14:22:28', '2026-10-30 14:22:28');
INSERT INTO `blog_access_token` VALUES (37, 1, '1dab918d603d3901366e358113bc233dcb02d7ed61295b9c54c22d9226139932', '2026-11-06 14:22:47', '2026-10-30 14:22:47');
INSERT INTO `blog_access_token` VALUES (38, 1, 'ec7f21b488fac4744d04a70ffb1131e684598b28a9b4b43db6cff15b2605b83c', '2026-11-06 14:24:12', '2026-10-30 14:24:12');
INSERT INTO `blog_access_token` VALUES (39, 1, 'dfeb2d1537113f47532901383e601f55e668829cd8798b6a9b499dd3fd419a01', '2026-11-29 14:24:35', '2026-10-30 14:24:35');
INSERT INTO `blog_access_token` VALUES (40, 1, '619f5015dd1a144c9edea829c834a9b1412e7bd322bd5cf330bbba231d2e14f0', '2026-11-06 14:35:24', '2026-10-30 14:35:24');
INSERT INTO `blog_access_token` VALUES (41, 1, '40c7e3ae3ffb193a821bd89da98a5080df9399ffccf20a5c8d909e0813860bc5', '2026-11-06 14:36:33', '2026-10-30 14:36:33');
INSERT INTO `blog_access_token` VALUES (42, 1, 'f5419cb98d4470e216b608824de0d8736eb78692734904d4347977fbefc88ec6', '2026-11-06 14:37:41', '2026-10-30 14:37:41');
INSERT INTO `blog_access_token` VALUES (43, 1, '345d87e47d829255af29385b4e62a56197a56a2b20231277722498ef5e66782d', '2026-11-29 14:39:05', '2026-10-30 14:39:05');
INSERT INTO `blog_access_token` VALUES (44, 1, 'a11c5080f57149565d79aa857d03ddf52febb4affaca7b6e40352742c0eb524f', '2026-11-06 14:39:26', '2026-10-30 14:39:26');
INSERT INTO `blog_access_token` VALUES (45, 1, '795c5700ded523328ef5b91a9a6d9c1f0b628db348d3451cc219e3167fa2ac8a', '2026-11-06 15:00:08', '2026-10-30 15:00:08');
INSERT INTO `blog_access_token` VALUES (46, 1, '9e3522c11419d17cc573ad7ce6e90738bdbb4b622636756c3a227df9a3afb981', '2026-11-06 15:02:48', '2026-10-30 15:02:48');
INSERT INTO `blog_access_token` VALUES (47, 1, '2ebcaaf677d61896b877de7813cdba598ca0241c3bc410c3b0996fc05d7c5703', '2026-11-06 15:06:31', '2026-10-30 15:06:31');
INSERT INTO `blog_access_token` VALUES (48, 1, 'b0ee1bbcbede54b5b1786b19452f33afd11545953ea5f689f2f0b92e92c71b15', '2026-11-06 15:06:48', '2026-10-30 15:06:48');
INSERT INTO `blog_access_token` VALUES (49, 2, '9d0c16fa6c541f368dcc5a67712f1a3ae05e7fb5669329ebf0e5a62064964845', '2026-11-06 15:07:31', '2026-10-30 15:07:31');
INSERT INTO `blog_access_token` VALUES (50, 1, 'e2ad4f1492c3ad386e25e8c101dfec84c720027495a2689b5d9ffc86583b4e6a', '2026-11-06 15:07:47', '2026-10-30 15:07:47');
INSERT INTO `blog_access_token` VALUES (51, 1, '5acb9d22907fff52c734e1c07cce0e48fea09032c0fa68fa972cf8c037943756', '2026-11-06 15:08:24', '2026-10-30 15:08:24');
INSERT INTO `blog_access_token` VALUES (52, 1, '04b3fc311cc85ec4702f5bb69b2f12ff5577bd6d7a368282c4346b0fb176ed2c', '2026-11-06 15:10:27', '2026-10-30 15:10:27');

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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章分类' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_comment
-- ----------------------------
INSERT INTO `blog_comment` VALUES (1, 6, 2, NULL, '根本.....没有这种图片', 'NORMAL', '2026-10-22 14:33:59', '2026-10-26 09:34:50', 1);
INSERT INTO `blog_comment` VALUES (2, 6, 1, NULL, '这篇写得有意思', 'NORMAL', '2026-10-26 09:09:35', '2026-10-30 15:01:02', 1);
INSERT INTO `blog_comment` VALUES (3, 6, 1, NULL, '同感，尤其第二段', 'NORMAL', '2026-10-26 09:09:35', '2026-10-30 15:01:04', 1);
INSERT INTO `blog_comment` VALUES (4, 6, 1, NULL, '你只是怕了', 'NORMAL', '2026-10-26 09:13:01', '2026-10-30 15:01:05', 1);
INSERT INTO `blog_comment` VALUES (5, 6, 1, NULL, '删除测试-父', 'NORMAL', '2026-10-26 09:15:38', '2026-10-26 09:15:39', 1);
INSERT INTO `blog_comment` VALUES (6, 6, 1, 5, '删除测试-子', 'NORMAL', '2026-10-26 09:15:39', '2026-10-26 09:15:39', 1);
INSERT INTO `blog_comment` VALUES (7, 6, 1, NULL, '测试楼层：贴吧式盖楼效果验证', 'NORMAL', '2026-10-30 15:06:31', '2026-10-30 15:08:24', 1);
INSERT INTO `blog_comment` VALUES (8, 6, 2, NULL, '作者本人来冒个泡，验证楼主标识~', 'NORMAL', '2026-10-30 15:07:31', '2026-10-30 15:08:24', 1);

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
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '关注关系' ROW_FORMAT = DYNAMIC;

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
  `quote_id` bigint NULL DEFAULT NULL COMMENT '被引用的消息 id',
  `recalled` tinyint NOT NULL DEFAULT 0 COMMENT '是否已撤回（双方可见）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_blog_message_conversation`(`sender_id` ASC, `receiver_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_blog_message_unread`(`receiver_id` ASC, `read_flag` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私信' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_message
-- ----------------------------
INSERT INTO `blog_message` VALUES (1, 2, 1, 'hello', 1, 0, 0, NULL, 0, '2026-10-21 16:30:42');
INSERT INTO `blog_message` VALUES (2, 2, 1, '😍', 1, 0, 0, NULL, 0, '2026-10-21 16:30:51');
INSERT INTO `blog_message` VALUES (3, 1, 2, '', 1, 0, 0, NULL, 1, '2026-10-21 16:33:31');
INSERT INTO `blog_message` VALUES (4, 1, 2, '', 1, 0, 0, NULL, 1, '2026-10-26 09:09:15');
INSERT INTO `blog_message` VALUES (5, 1, 2, '', 1, 0, 0, 4, 1, '2026-10-26 09:09:16');
INSERT INTO `blog_message` VALUES (7, 2, 1, '?', 1, 0, 0, 5, 0, '2026-10-26 09:27:23');
INSERT INTO `blog_message` VALUES (8, 2, 1, '✅', 1, 0, 0, NULL, 0, '2026-10-26 09:32:57');
INSERT INTO `blog_message` VALUES (9, 1, 2, '', 1, 0, 0, NULL, 1, '2026-10-26 09:36:10');
INSERT INTO `blog_message` VALUES (10, 2, 1, '', 1, 0, 1, NULL, 1, '2026-10-26 09:36:12');
INSERT INTO `blog_message` VALUES (11, 1, 2, '啊呀。', 1, 0, 0, NULL, 0, '2026-10-26 09:40:16');

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
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '站内通知' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_notification
-- ----------------------------
INSERT INTO `blog_notification` VALUES (4, 1, 2, 'FOLLOW', NULL, NULL, NULL, 1, '2026-10-21 16:28:52');
INSERT INTO `blog_notification` VALUES (6, 2, 1, 'FOLLOW', NULL, NULL, NULL, 1, '2026-10-21 16:46:16');
INSERT INTO `blog_notification` VALUES (7, 2, 1, 'LIKE', 6, NULL, NULL, 1, '2026-10-26 08:54:26');
INSERT INTO `blog_notification` VALUES (8, 2, 1, 'COMMENT', 6, 2, '这篇写得有意思', 1, '2026-10-26 09:09:35');
INSERT INTO `blog_notification` VALUES (9, 2, 1, 'COMMENT', 6, 3, '同感，尤其第二段', 1, '2026-10-26 09:09:35');
INSERT INTO `blog_notification` VALUES (10, 2, 1, 'REPLY', 6, 4, '你只是怕了', 1, '2026-10-26 09:13:01');
INSERT INTO `blog_notification` VALUES (11, 2, 1, 'COMMENT', 6, 4, '你只是怕了', 1, '2026-10-26 09:13:01');
INSERT INTO `blog_notification` VALUES (12, 2, 1, 'COMMENT', 6, 5, '删除测试-父', 1, '2026-10-26 09:15:38');
INSERT INTO `blog_notification` VALUES (13, 2, 1, 'COMMENT', 6, 6, '删除测试-子', 1, '2026-10-26 09:15:39');
INSERT INTO `blog_notification` VALUES (14, 2, 1, 'COMMENT', 6, 7, '测试楼层：贴吧式盖楼效果验证', 0, '2026-10-30 15:06:31');

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
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_post
-- ----------------------------
INSERT INTO `blog_post` VALUES (1, 1, 1, '状态自由测试', 'post-1792649378742', '', '正文内容测试', '', '置顶推荐', 0, 0, 0, NULL, '2026-10-22 14:09:39', '2026-10-22 14:09:39', '2026-10-22 14:10:16', 1);
INSERT INTO `blog_post` VALUES (2, 1, 1, '草稿测试', 'post-1792649405126', '', '草稿正文', '', 'DRAFT', 0, 0, 0, NULL, NULL, '2026-10-22 14:10:05', '2026-10-22 14:10:16', 1);
INSERT INTO `blog_post` VALUES (3, 1, 1, 'A-草稿', 'a', '', '正文', '', 'DRAFT', 0, 0, 0, NULL, NULL, '2026-10-22 14:20:39', '2026-10-22 14:20:54', 1);
INSERT INTO `blog_post` VALUES (4, 1, 1, 'B-已发布', 'b', '', '正文', '', 'PUBLISHED', 0, 0, 0, NULL, '2026-10-22 14:20:40', '2026-10-22 14:20:40', '2026-10-22 14:20:54', 1);
INSERT INTO `blog_post` VALUES (5, 1, 1, 'C-自定义', 'c', '', '正文', '', '置顶推荐', 0, 0, 0, NULL, '2026-10-22 14:20:40', '2026-10-22 14:20:40', '2026-10-22 14:20:54', 1);
INSERT INTO `blog_post` VALUES (6, 2, 2, '这里是67广场', '67', '', '你必须吃掉这些东西\n![修改自称.png](/uploads/2026/10/0ab38a18396f44ad81a26cea1ba84973.png)\n', '', 'PUBLISHED', 27, 2, 0, NULL, '2026-10-22 14:25:44', '2026-10-22 14:25:44', '2026-10-30 15:07:51', 0);
INSERT INTO `blog_post` VALUES (7, 1, 3, '进站以来第一个文章', 'post-1792976383340', '', 'hello！我是青禾\n![mascot-welcome-BE0nkIi0.png](/uploads/2026/10/1d8f6dfc7fa74e54ab9f1f0356ed31e9.png)\n', '', 'PUBLISHED', 8, 1, 0, NULL, '2026-10-26 08:59:43', '2026-10-26 08:59:43', '2026-10-30 15:01:10', 0);

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
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章点赞' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_post_like
-- ----------------------------
INSERT INTO `blog_post_like` VALUES (1, 6, 2, '2026-10-22 14:28:40');
INSERT INTO `blog_post_like` VALUES (2, 6, 1, '2026-10-26 08:54:26');
INSERT INTO `blog_post_like` VALUES (3, 7, 1, '2026-10-26 09:41:24');

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章标签关联' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章标签' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of blog_user
-- ----------------------------
INSERT INTO `blog_user` VALUES (1, 'qinghe', '$2a$10$grTvthiFX81jmF3M.8VjXeD2rIAhULIzO9pHleDfBGveDgEYKHOqO', '青禾', 'qinghe@fenglin.blog', '/uploads/2026/10/1f62d6c82f0444cd8486e93e42fc82df.png', '', 'ninja', 'ADMIN', 1, NULL, NULL, '2026-10-21 16:08:12', '2026-10-30 14:11:34');
INSERT INTO `blog_user` VALUES (2, 'jingling', '$2a$10$YgoogTThK3yCawTa8B50/eOAZDjrVLI8Tm8aJ.IBrFGdQ.rxmflsC', '景翎', 'jingling@fenglin.blog', 'ninja:0', '', 'ninja', 'ADMIN', 1, NULL, NULL, '2026-10-21 16:08:12', '2026-10-21 16:08:12');

SET FOREIGN_KEY_CHECKS = 1;
