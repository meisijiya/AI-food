-- ============================================
-- V2: 添加乐观锁 version 字段（MyBatis-Plus @Version）
-- 兼容 JPA 时代存量数据：DEFAULT 0
-- 16 张业务表全部添加
-- ============================================

ALTER TABLE conversation_session ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE qa_record ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE collected_params ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE recommendation_result ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE photo ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE share_record ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE feed_post ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE feed_comment ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE user_follow ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE chat_conversation ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE chat_message ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE chat_photo ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE chat_file ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE user_bloom_filter ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE bloom_sync_log ADD COLUMN version INT NOT NULL DEFAULT 0;
