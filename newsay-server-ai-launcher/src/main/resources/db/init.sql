-- 创建数据库
CREATE DATABASE IF NOT EXISTS newsay_chat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE newsay_chat;

-- 创建对话历史表
CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL UNIQUE COMMENT '对话唯一标识',
    messages JSON NOT NULL COMMENT '对话消息列表，JSON数组',
    last_access_time DATETIME NOT NULL COMMENT '最后访问时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_last_access_time (last_access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史表';
