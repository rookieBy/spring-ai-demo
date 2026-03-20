-- Create database if not exists
CREATE DATABASE IF NOT EXISTS spring_ai_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE spring_ai_demo;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    status INT DEFAULT 1 COMMENT '1:active, 0:inactive',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample data
INSERT INTO users (username, email, status) VALUES
    ('admin', 'admin@example.com', 1),
    ('user1', 'user1@example.com', 1),
    ('user2', 'user2@example.com', 1)
ON DUPLICATE KEY UPDATE username=username;
