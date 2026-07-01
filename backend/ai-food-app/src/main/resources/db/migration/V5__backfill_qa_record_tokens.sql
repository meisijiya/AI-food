-- V5: 一次性回填旧 qa_record 的 token 字段
--
-- 背景:V4 加了 prompt_tokens / completion_tokens / total_tokens 字段,
--      但旧 qa_record 记录这些字段为 NULL,导致 admin 看板 sum() ≈ 0。
--
-- 估算策略:每 2 个字符约 1 个 token(中英文混合保守值)。
--   - prompt_tokens     = char_count(user_answer)  / 2
--   - completion_tokens = char_count(ai_question) / 2
--   - total_tokens      = prompt + completion
--
-- 注意:这是估算值,仅用于 admin 看板展示趋势,**不用于计费**。
--      准确 token 需按模型 tokenize,那是 P2(本脚本回避过度工程)。
--
-- 范围:仅回填 is_deleted=0 且 total_tokens 为空/0 的记录,
--      已有正确数据的记录不触碰(幂等)。

UPDATE qa_record
SET
    prompt_tokens     = LEAST(65535, GREATEST(0, CAST(CHAR_LENGTH(IFNULL(user_answer,  '')) / 2 AS UNSIGNED))),
    completion_tokens = LEAST(65535, GREATEST(0, CAST(CHAR_LENGTH(IFNULL(ai_question, '')) / 2 AS UNSIGNED))),
    total_tokens      = LEAST(65535, GREATEST(0,
                          CAST(CHAR_LENGTH(IFNULL(user_answer,  '')) / 2 AS UNSIGNED)
                        + CAST(CHAR_LENGTH(IFNULL(ai_question, '')) / 2 AS UNSIGNED)
                    ))
WHERE is_deleted = 0
  AND (total_tokens IS NULL OR total_tokens = 0);
