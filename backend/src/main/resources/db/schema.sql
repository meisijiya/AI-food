-- AI美食推荐应用数据库初始化脚本

CREATE DATABASE IF NOT EXISTS ai_food DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_food;

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
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id)
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
    INDEX idx_session (session_id)
);

-- 参数收集结果表
CREATE TABLE IF NOT EXISTS collected_params (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    param_name VARCHAR(50),
    param_value TEXT,
    param_type VARCHAR(20) COMMENT 'required/optional',
    collected_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_session_param (session_id, param_name)
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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id)
);