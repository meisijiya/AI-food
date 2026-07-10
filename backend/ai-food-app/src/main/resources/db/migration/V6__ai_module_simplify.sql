-- ============================================================
-- V6: AI 模块简化
-- spec: docs/superpowers/specs/2026-07-07-ai-module-simplify-design.md §3.7
-- 包含：
--   1. recommendation_result: 删 3 字段 + 加 3 字段
--   2. qa_record: 加 user_id 字段 + 复合索引
-- ============================================================

-- 1. recommendation_result 字段调整
ALTER TABLE recommendation_result
    DROP COLUMN mode,
    DROP COLUMN old_food,
    DROP COLUMN similarity_score;

ALTER TABLE recommendation_result
    ADD COLUMN category VARCHAR(64) DEFAULT NULL COMMENT '菜系/品类' AFTER reason,
    ADD COLUMN flavor_tags JSON DEFAULT NULL COMMENT '口味标签 JSON 数组' AFTER category,
    ADD COLUMN total_tokens INT DEFAULT NULL COMMENT '本次推荐消耗 token' AFTER flavor_tags;

-- 2. qa_record 加 user_id 字段（每日限额聚合用）
ALTER TABLE qa_record
    ADD COLUMN user_id BIGINT DEFAULT NULL COMMENT '用户ID（关联 sys_user）' AFTER session_id,
    ADD INDEX idx_user_created (user_id, created_at, total_tokens);
