-- 简化的用户系统数据库设计
CREATE DATABASE IF NOT EXISTS `aidemo1` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `aidemo1`;

-- 用户表 - 简化设计，去掉不必要的农业相关字段
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` varchar(50) NOT NULL COMMENT '昵称',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `address` varchar(200) DEFAULT NULL COMMENT '用户地址（城市，供AI天气查询使用）',
  `status` tinyint DEFAULT 1 COMMENT '状态：1正常，0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 测试用户数据
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `phone`, `email`, `address`, `status`) VALUES
('admin', '$2a$10$3EyqUalJVxkpbfdG8OsTM.K3jaZesSt3wQRCoKWVPChw8m4e8SPQ.', '管理员', '13900139000', 'admin@example.com', '长沙', 1),
('test', '$2a$10$3EyqUalJVxkpbfdG8OsTM.K3jaZesSt3wQRCoKWVPChw8m4e8SPQ.', '测试用户', '13800138000', 'test@example.com', '南京', 1),
('demo', '$2a$10$3EyqUalJVxkpbfdG8OsTM.K3jaZesSt3wQRCoKWVPChw8m4e8SPQ.', '演示用户', '13700137000', 'demo@example.com', '广州', 1);

-- 注意：上面密码的明文对应关系
-- admin: admin123
-- test: admin123
-- demo: admin123

-- 用户统计表 - 简化为通用统计
DROP TABLE IF EXISTS `sys_user_stats`;
CREATE TABLE `sys_user_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `login_count` int DEFAULT 0 COMMENT '登录次数',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  CONSTRAINT `fk_user_stats_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计表';

-- 初始化统计数据
INSERT INTO `sys_user_stats` (`user_id`, `login_count`, `last_login_time`) VALUES
(1, 5, '2024-01-15 10:30:00'),
(2, 8, '2024-01-16 14:20:00'),
(3, 3, '2024-01-17 09:15:00');

-- 病虫害检测记录表
DROP TABLE IF EXISTS `detection_records`;
CREATE TABLE `detection_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `detection_id` varchar(100) NOT NULL COMMENT '检测ID（前端生成）',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（可为空，支持游客检测）',
  `detected_diseases` json NOT NULL COMMENT '检测到的病虫害（JSON格式）',
  `confidence` decimal(5,4) DEFAULT 0.0000 COMMENT '检测置信度',
  `ai_diagnosis` text COMMENT 'AI诊断结果',
  `is_saved` tinyint DEFAULT 0 COMMENT '是否用户主动收藏：1是，0否',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_detection_id` (`detection_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_detection_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病虫害检测记录表';






