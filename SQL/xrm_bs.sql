/*
SQLyog Community v12.09 (64 bit)
MySQL - 8.0.36 : Database - xrm_bs
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`xrm_bs` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `xrm_bs`;

/*Table structure for table `article_settings` */

DROP TABLE IF EXISTS `article_settings`;

CREATE TABLE `article_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `article_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `setting_id` bigint NOT NULL,
  PRIMARY KEY (`id`,`article_id`,`setting_id`),
  UNIQUE KEY `UKiy7ofhfrfxhwicbb1cglqfn58` (`article_id`,`setting_id`),
  KEY `FKa8ybsf47pwu8i7kuxscckt289` (`setting_id`),
  CONSTRAINT `FKa8ybsf47pwu8i7kuxscckt289` FOREIGN KEY (`setting_id`) REFERENCES `settings` (`id`),
  CONSTRAINT `FKkd2xji8yxl8k2tbo6ahvlfvk2` FOREIGN KEY (`article_id`) REFERENCES `articles` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=342 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `article_tags` */

DROP TABLE IF EXISTS `article_tags`;

CREATE TABLE `article_tags` (
  `article_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`article_id`,`tag_id`),
  KEY `tag_id` (`tag_id`),
  CONSTRAINT `article_tags_ibfk_1` FOREIGN KEY (`article_id`) REFERENCES `articles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `article_tags_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `articles` */

DROP TABLE IF EXISTS `articles`;

CREATE TABLE `articles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章标题',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章内容（使用LONGTEXT支持长文本）',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `status` enum('draft','published') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'draft' COMMENT '状态：草稿/已发布',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `parent_article_id` bigint DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `author_id` (`author_id`),
  KEY `fk_articles_parent` (`parent_article_id`),
  CONSTRAINT `articles_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_articles_parent` FOREIGN KEY (`parent_article_id`) REFERENCES `articles` (`id`) ON DELETE SET NULL,
  CONSTRAINT `chk_article_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'PUBLISHED')))
) ENGINE=InnoDB AUTO_INCREMENT=345 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `carousel_ads` */

DROP TABLE IF EXISTS `carousel_ads`;

CREATE TABLE `carousel_ads` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL,
  `image_url` varchar(255) NOT NULL,
  `link_url` varchar(255) NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `comment_likes` */

DROP TABLE IF EXISTS `comment_likes`;

CREATE TABLE `comment_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`),
  KEY `fk_comment_likes_user` (`user_id`),
  CONSTRAINT `fk_comment_likes_comment` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=193 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `comments` */

DROP TABLE IF EXISTS `comments`;

CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标类型: article, setting, tag',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID，NULL表示一级评论',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `status` int DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_user` (`user_id`),
  CONSTRAINT `fk_comments_parent` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=576 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `favorite_folders` */

DROP TABLE IF EXISTS `favorite_folders`;

CREATE TABLE `favorite_folders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `type` varchar(20) NOT NULL DEFAULT 'article',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_folder` (`user_id`,`name`),
  UNIQUE KEY `UK4qlcau1li44a4huugrecy54t3` (`user_id`,`name`),
  CONSTRAINT `favorite_folders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `favorite_items` */

DROP TABLE IF EXISTS `favorite_items`;

CREATE TABLE `favorite_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `folder_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `item_type` varchar(20) NOT NULL DEFAULT 'article' COMMENT 'article 或 setting',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKknjdcr8ref2hfityy6d5xtkwe` (`folder_id`,`item_id`),
  UNIQUE KEY `uk_folder_item` (`folder_id`,`item_type`,`item_id`),
  KEY `article_id` (`item_id`),
  CONSTRAINT `favorite_items_ibfk_1` FOREIGN KEY (`folder_id`) REFERENCES `favorite_folders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=381 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `notifications` */

DROP TABLE IF EXISTS `notifications`;

CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收通知的用户ID',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知类型: like_article, like_setting, like_comment, comment_article, comment_setting, comment_tag, reply_comment, favorite_article, favorite_setting',
  `source_user_id` bigint NOT NULL COMMENT '触发通知的用户ID',
  `target_id` bigint NOT NULL COMMENT '目标ID（文章ID/设定ID/评论ID等）',
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标类型: article, setting, comment, tag',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '通知内容简述',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `comment_id` bigint DEFAULT NULL COMMENT '关联的评论ID（仅评论相关通知）',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`,`is_read`),
  KEY `idx_user_created` (`user_id`,`created_at`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=606 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `recent_views` */

DROP TABLE IF EXISTS `recent_views`;

CREATE TABLE `recent_views` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `viewed_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `item_type` varchar(20) NOT NULL DEFAULT 'article' COMMENT 'article 或 setting',
  PRIMARY KEY (`id`),
  KEY `article_id` (`item_id`),
  KEY `idx_user_viewed` (`user_id`,`viewed_at` DESC),
  CONSTRAINT `recent_views_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2579 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `setting_favorite_items` */

DROP TABLE IF EXISTS `setting_favorite_items`;

CREATE TABLE `setting_favorite_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `setting_id` bigint NOT NULL,
  `folder_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKk4tktlk6kxs3a64ovpm4k0b2d` (`folder_id`),
  CONSTRAINT `FKk4tktlk6kxs3a64ovpm4k0b2d` FOREIGN KEY (`folder_id`) REFERENCES `favorite_folders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `setting_tags` */

DROP TABLE IF EXISTS `setting_tags`;

CREATE TABLE `setting_tags` (
  `setting_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`setting_id`,`tag_id`),
  KEY `tag_id` (`tag_id`),
  CONSTRAINT `setting_tags_ibfk_1` FOREIGN KEY (`setting_id`) REFERENCES `settings` (`id`) ON DELETE CASCADE,
  CONSTRAINT `setting_tags_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `settings` */

DROP TABLE IF EXISTS `settings`;

CREATE TABLE `settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设定标题',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设定详细内容',
  `category` enum('character','item','location','background','rule','custom') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设定分类',
  `template_data` json DEFAULT NULL COMMENT '分类专属字段（JSON结构）',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `status` enum('draft','published') COLLATE utf8mb4_unicode_ci DEFAULT 'draft' COMMENT '状态',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `parent_setting_id` bigint DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `author_id` (`author_id`),
  KEY `fk_settings_parent` (`parent_setting_id`),
  CONSTRAINT `fk_settings_parent` FOREIGN KEY (`parent_setting_id`) REFERENCES `settings` (`id`) ON DELETE SET NULL,
  CONSTRAINT `settings_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=198 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `tag_versions` */

DROP TABLE IF EXISTS `tag_versions`;

CREATE TABLE `tag_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tag_id` bigint NOT NULL,
  `version` int NOT NULL,
  `name` varchar(50) NOT NULL,
  `description` longtext,
  `classification` varchar(20) NOT NULL,
  `color` varchar(20) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tag_id` (`tag_id`),
  CONSTRAINT `tag_versions_ibfk_1` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `tags` */

DROP TABLE IF EXISTS `tags`;

CREATE TABLE `tags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称（唯一）',
  `description` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `usage_count` int DEFAULT '0',
  `is_recommended` tinyint(1) DEFAULT '0',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `classification` enum('article','character','item','location','background','rule','custom') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签所属分类',
  `setting_category` enum('character','item','location','background','rule','custom') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` enum('article','setting') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name_type` (`name`,`type`)
) ENGINE=InnoDB AUTO_INCREMENT=181 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `user_behavior_logs` */

DROP TABLE IF EXISTS `user_behavior_logs`;

CREATE TABLE `user_behavior_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(30) NOT NULL,
  `action_time` datetime(6) DEFAULT NULL,
  `item_id` bigint NOT NULL,
  `item_type` varchar(20) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=297 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `user_blocked_tags` */

DROP TABLE IF EXISTS `user_blocked_tags`;

CREATE TABLE `user_blocked_tags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj5b404bewjy6sqv7h77mtwjvd` (`tag_id`),
  CONSTRAINT `FKj5b404bewjy6sqv7h77mtwjvd` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `user_favorites` */

DROP TABLE IF EXISTS `user_favorites`;

CREATE TABLE `user_favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `article_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe2eifcdi205s7lc149m3sq42g` (`user_id`,`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `user_follow` */

DROP TABLE IF EXISTS `user_follow`;

CREATE TABLE `user_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `follower_id` bigint NOT NULL COMMENT '关注者用户ID',
  `followee_id` bigint NOT NULL COMMENT '被关注者用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_followee` (`follower_id`,`followee_id`),
  UNIQUE KEY `UK6jt2iqjk3gue98jdntdulpopf` (`follower_id`,`followee_id`),
  KEY `idx_followee_id` (`followee_id`)
) ENGINE=InnoDB AUTO_INCREMENT=194 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `user_likes` */

DROP TABLE IF EXISTS `user_likes`;

CREATE TABLE `user_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article_like` (`user_id`,`article_id`),
  UNIQUE KEY `UKcx9d48xbw565qra29tfc5ofp4` (`user_id`,`article_id`),
  KEY `article_id` (`article_id`),
  CONSTRAINT `user_likes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_likes_ibfk_2` FOREIGN KEY (`article_id`) REFERENCES `articles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=304 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `user_preferred_tags` */

DROP TABLE IF EXISTS `user_preferred_tags`;

CREATE TABLE `user_preferred_tags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7v4ggd1el5fr1c792i07238qc` (`tag_id`),
  CONSTRAINT `FK7v4ggd1el5fr1c792i07238qc` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=190 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `user_setting_likes` */

DROP TABLE IF EXISTS `user_setting_likes`;

CREATE TABLE `user_setting_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `setting_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_setting` (`user_id`,`setting_id`),
  KEY `setting_id` (`setting_id`),
  CONSTRAINT `fk_user_setting_likes_setting` FOREIGN KEY (`setting_id`) REFERENCES `settings` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_setting_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=156 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `users` */

DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  `role` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
