-- ============================================================
-- V7: Token 限额配置表
-- spec: docs/superpowers/specs/2026-07-07-ai-module-simplify-design.md §10.3
-- 包含：
--   1. system_config 表（key-value 系统配置）
--   2. user_token_quota 表（per-user 限额覆盖）
--   3. 初始 seed: daily_token_limit_default = 1000000
-- ============================================================

CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(64) NOT NULL UNIQUE COMMENT '配置 key',
    config_value VARCHAR(255) NOT NULL COMMENT '配置 value',
    description VARCHAR(255) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '系统级配置';

CREATE TABLE user_token_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    daily_token_limit INT NOT NULL COMMENT '每日 token 限额（per user）',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户 token 限额覆盖';

-- 初始默认 token 限额 1,000,000/天
INSERT INTO system_config (config_key, config_value, description)
VALUES ('daily_token_limit_default', '1000000', '默认每日 token 限额（per user）');
