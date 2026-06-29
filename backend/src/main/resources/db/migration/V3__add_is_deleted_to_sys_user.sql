-- V3: 给 sys_user 加 is_deleted 列（修复 MyBatis-Plus @TableLogic 自动注入 "AND is_deleted = 0" 找不到列）
-- TINYINT(1) NOT NULL DEFAULT 0 — 已存在用户行默认值 0（未删除）
ALTER TABLE sys_user ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER version;
