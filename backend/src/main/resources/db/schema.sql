-- AI美食推荐应用数据库初始化脚本
-- Spring Boot 启动时自动执行，CREATE TABLE IF NOT EXISTS 确保幂等

-- 用户会话表
CREATE TABLE IF NOT EXISTS conversation_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) UNIQUE NOT NULL,
    user_id BIGINT,
    total_questions INT,
    current_question_count INT DEFAULT 0,
    interrupt_count INT DEFAULT 0,
    mode VARCHAR(20) COMMENT 'inertia/random',
    status VARCHAR(20) DEFAULT 'active',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at DATETIME,
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_is_deleted (is_deleted)
);

-- 问答记录表
CREATE TABLE IF NOT EXISTS qa_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    question_type VARCHAR(20) COMMENT 'question/chat/2question/interrupt',
    param_name VARCHAR(50),
    ai_question TEXT,
    user_answer TEXT,
    is_valid BOOLEAN DEFAULT TRUE,
    question_order INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_session (session_id),
    INDEX idx_is_deleted (is_deleted)
);

-- 参数收集结果表
CREATE TABLE IF NOT EXISTS collected_params (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    param_name VARCHAR(50),
    param_value TEXT,
    param_type VARCHAR(20) COMMENT 'required/optional',
    collected_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    UNIQUE KEY uk_session_param (session_id, param_name),
    INDEX idx_is_deleted (is_deleted)
);

-- 推荐结果表
CREATE TABLE IF NOT EXISTS recommendation_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    mode VARCHAR(20),
    food_name VARCHAR(100),
    old_food VARCHAR(100) COMMENT '随机模式的旧值',
    similarity_score DECIMAL(3,2),
    reason TEXT,
    photo_url VARCHAR(500),
    comment TEXT COMMENT '用户对打卡美食的评价',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_session (session_id),
    INDEX idx_is_deleted (is_deleted)
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- 照片表
CREATE TABLE IF NOT EXISTS photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    original_path VARCHAR(500) NOT NULL COMMENT '原图路径',
    thumbnail_path VARCHAR(500) COMMENT '缩略图路径',
    related_session_id VARCHAR(64) COMMENT '关联的会话ID',
    file_name VARCHAR(255),
    original_size BIGINT COMMENT '原图大小(bytes)',
    thumbnail_size BIGINT COMMENT '缩略图大小(bytes)',
    mime_type VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_user_id (user_id),
    INDEX idx_session (related_session_id)
);

-- 分享记录表
CREATE TABLE IF NOT EXISTS share_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    share_token VARCHAR(64) UNIQUE NOT NULL COMMENT '分享链接token',
    user_id BIGINT NOT NULL COMMENT '分享者用户ID',
    session_id VARCHAR(64) NOT NULL COMMENT '关联的会话ID',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_share_token (share_token),
    INDEX idx_user_id (user_id)
);

-- 大厅发布帖子表
CREATE TABLE IF NOT EXISTS feed_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    food_name VARCHAR(100),
    comment_preview VARCHAR(100) COMMENT '评论前30字预览',
    thumbnail_url VARCHAR(500),
    original_photo_url VARCHAR(500),
    reason TEXT,
    collected_params JSON COMMENT '收集参数快照',
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    visibility VARCHAR(20) DEFAULT 'public' COMMENT 'public/friends',
    published_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_published_at (published_at DESC),
    INDEX idx_food_name (food_name),
    INDEX idx_visibility (visibility)
);

-- 大厅评论表
CREATE TABLE IF NOT EXISTS feed_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_post_id (post_id, created_at DESC),
    INDEX idx_user_id (user_id)
);

-- 用户关注表
CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL COMMENT '关注者ID',
    following_id BIGINT NOT NULL COMMENT '被关注者ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, following_id),
    INDEX idx_follower (follower_id),
    INDEX idx_following (following_id)
);

-- 聊天对话表
CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_key VARCHAR(128) NOT NULL COMMENT '对话唯一键: userId1_userId2排序拼接',
    user1_id BIGINT NOT NULL COMMENT '用户1 ID（较小的ID）',
    user2_id BIGINT NOT NULL COMMENT '用户2 ID（较大的ID）',
    last_message TEXT COMMENT '最后一条消息预览',
    last_message_at DATETIME COMMENT '最后消息时间',
    cleared_at_user1 DATETIME DEFAULT NULL COMMENT 'user1清除聊天的时间点，NULL表示未清除（消息过滤边界，永不重置）',
    cleared_at_user2 DATETIME DEFAULT NULL COMMENT 'user2清除聊天的时间点，NULL表示未清除（消息过滤边界，永不重置）',
    hidden_at_user1 DATETIME DEFAULT NULL COMMENT 'user1隐藏会话的时间点，新消息到达时重置（列表可见性控制）',
    hidden_at_user2 DATETIME DEFAULT NULL COMMENT 'user2隐藏会话的时间点，新消息到达时重置（列表可见性控制）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_key (conversation_key),
    INDEX idx_user1 (user1_id, last_message_at DESC),
    INDEX idx_user2 (user2_id, last_message_at DESC)
);

-- 迁移：添加新列（如果表已存在且只有旧字段）
-- ALTER TABLE chat_conversation
--   ADD COLUMN hidden_at_user1 DATETIME DEFAULT NULL,
--   ADD COLUMN hidden_at_user2 DATETIME DEFAULT NULL;

-- 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL COMMENT '对话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type VARCHAR(20) DEFAULT 'text' COMMENT '消息类型: text/image/file',
    photo_id BIGINT COMMENT '关联聊天照片ID',
    file_id BIGINT COMMENT '关联聊天文件ID',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '软删除标记',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id, created_at DESC),
    INDEX idx_receiver_read (receiver_id, is_read),
    INDEX idx_sender (sender_id),
    INDEX idx_deleted (is_deleted)
);

-- 聊天照片表（30天自动过期）
CREATE TABLE IF NOT EXISTS chat_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT COMMENT '关联对话ID（发送消息后更新）',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    original_path VARCHAR(500) NOT NULL COMMENT '原图路径',
    thumbnail_path VARCHAR(500) COMMENT '缩略图路径',
    file_name VARCHAR(255) COMMENT '原始文件名',
    original_size BIGINT COMMENT '原图大小(bytes)',
    thumbnail_size BIGINT COMMENT '缩略图大小(bytes)',
    mime_type VARCHAR(50) COMMENT 'MIME类型',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '软删除标记（保留，兼容旧逻辑）',
    is_receiver_delete BOOLEAN DEFAULT FALSE COMMENT '接收者删除标记',
    is_sender_delete BOOLEAN DEFAULT FALSE COMMENT '发送者删除标记',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id),
    INDEX idx_sender (sender_id),
    INDEX idx_created (created_at),
    INDEX idx_deleted (is_deleted)
);

-- 聊天文件表（30天自动过期，50MB限制）
CREATE TABLE IF NOT EXISTS chat_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT COMMENT '关联对话ID（发送消息后更新）',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    original_name VARCHAR(255) COMMENT '原始文件名',
    file_size BIGINT COMMENT '文件大小(bytes)',
    mime_type VARCHAR(50) COMMENT 'MIME类型',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '软删除标记（保留，兼容旧逻辑）',
    is_receiver_delete BOOLEAN DEFAULT FALSE COMMENT '接收者删除标记',
    is_sender_delete BOOLEAN DEFAULT FALSE COMMENT '发送者删除标记',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id),
    INDEX idx_sender (sender_id),
    INDEX idx_created (created_at),
    INDEX idx_deleted (is_deleted)
);
