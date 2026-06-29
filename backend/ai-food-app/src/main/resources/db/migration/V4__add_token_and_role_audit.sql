-- 1. qa_record 加 token 用量字段
--    ponytail: brief's idx_qa_created_user(created_at, user_id) skipped — qa_record
--    is keyed by session_id (no user_id column). created_at index added instead
--    since V1 already has idx_session for session lookups.
ALTER TABLE qa_record
    ADD COLUMN prompt_tokens INT NULL,
    ADD COLUMN completion_tokens INT NULL,
    ADD COLUMN total_tokens INT NULL,
    ADD COLUMN model VARCHAR(32) NULL;
CREATE INDEX idx_qa_created_at ON qa_record(created_at);

-- 2. sys_user 加 role 字段（admin 鉴权用）
ALTER TABLE sys_user ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER';

-- 3. admin_audit_log 表
CREATE TABLE admin_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    actor_username VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NULL,
    target_id VARCHAR(64) NULL,
    payload TEXT NULL,
    ip VARCHAR(64) NULL,
    user_agent VARCHAR(255) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_actor (actor_id, created_at),
    INDEX idx_audit_target (target_type, target_id, created_at),
    INDEX idx_audit_action (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 把现有唯一用户 smokeuser 提权为 ADMIN（user_id=1）
UPDATE sys_user SET role = 'ADMIN' WHERE id = 1;
