/*
 Navicat Premium Dump SQL

 Source Server         : 120.26.110.68_3306
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : 120.26.110.68:3306
 Source Schema         : evernight

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 11/10/2026 20:58:41
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for file
-- ----------------------------
DROP TABLE IF EXISTS `file`;
CREATE TABLE `file`  (
  `file_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `user_id` int UNSIGNED NOT NULL COMMENT '所属用户ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储路径',
  `file_type` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件类型',
  `file_size` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  `file_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件哈希(用于秒传)',
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除(0:正常,1:已删除)',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`file_id`) USING BTREE,
  INDEX `idx_user_parent`(`user_id` ASC) USING BTREE COMMENT '用户目录索引',
  INDEX `idx_user_deleted`(`user_id` ASC, `is_deleted` ASC) USING BTREE COMMENT '用户删除状态索引',
  CONSTRAINT `fk_file_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 66 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for forum_comment
-- ----------------------------
DROP TABLE IF EXISTS `forum_comment`;
CREATE TABLE `forum_comment`  (
  `comment_id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` int UNSIGNED NOT NULL COMMENT '帖子ID',
  `user_id` int UNSIGNED NOT NULL COMMENT '评论用户ID',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容(限500字)',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`comment_id`) USING BTREE,
  INDEX `idx_post_comment`(`post_id` ASC, `comment_id` ASC) USING BTREE COMMENT '帖子评论游标分页索引',
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '评论用户索引',
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE COMMENT '评论时间索引',
  CONSTRAINT `forum_comment_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `forum_comment_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`post_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '论坛评论表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for forum_post
-- ----------------------------
DROP TABLE IF EXISTS `forum_post`;
CREATE TABLE `forum_post`  (
  `post_id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `user_id` int UNSIGNED NOT NULL COMMENT '发帖用户ID',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `view_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览数',
  `post_status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '帖子状态(0:草稿,1:发布)',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`post_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '发帖用户索引',
  INDEX `idx_status_deleted`(`post_status` ASC) USING BTREE COMMENT '状态索引',
  CONSTRAINT `forum_post_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '论坛帖子表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `user_id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密密码(bcrypt)',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `user_status` tinyint UNSIGNED NOT NULL DEFAULT 2 COMMENT '用户状态(0:管理员,1:禁用,2:普通用户)',
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除(0:正常,1:已删除)',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE COMMENT '邮箱唯一索引',
  INDEX `idx_user_status`(`user_status` ASC) USING BTREE COMMENT '用户状态索引',
  INDEX `idx_login`(`email` ASC, `password` ASC) USING BTREE COMMENT '用户名密码索引'
) ENGINE = InnoDB AUTO_INCREMENT = 228 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
