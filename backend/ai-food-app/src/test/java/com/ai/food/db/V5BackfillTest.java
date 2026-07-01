package com.ai.food.db;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V5 回填 SQL 回归测试。
 * <p>
 * 纯文件内容测试,避免 SpringBootTest 启动慢。
 * 验证:V5 SQL 存在 + 包含对 qa_record 的 UPDATE + 引用三个 token 字段。
 */
class V5BackfillTest {

    private static final String V5_PATH = "/db/migration/V5__backfill_qa_record_tokens.sql";

    @Test
    void v5_sqlFile_exists_and_targets_qa_record_tokens() throws Exception {
        InputStream in = getClass().getResourceAsStream(V5_PATH);
        assertTrue(in != null, "V5 SQL 文件不存在于 classpath: " + V5_PATH);

        String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);

        // 回填目标表
        assertTrue(content.contains("UPDATE qa_record"),
            "V5 SQL 必须 UPDATE qa_record 表");

        // 三个 token 字段都得回填
        assertTrue(content.contains("prompt_tokens"),
            "V5 SQL 必须设置 prompt_tokens");
        assertTrue(content.contains("completion_tokens"),
            "V5 SQL 必须设置 completion_tokens");
        assertTrue(content.contains("total_tokens"),
            "V5 SQL 必须设置 total_tokens");

        // 字段名实际是 ai_question / user_answer(message/ai_response 在本表不存在)
        assertTrue(content.contains("ai_question"),
            "V5 SQL 必须用 ai_question 字段(completion 来源)");
        assertTrue(content.contains("user_answer"),
            "V5 SQL 必须用 user_answer 字段(prompt 来源)");

        // 范围过滤:幂等,只回填空数据
        assertTrue(content.contains("is_deleted"),
            "V5 SQL 必须按 is_deleted 过滤");
        assertTrue(content.contains("IS NULL"),
            "V5 SQL 必须处理 NULL total_tokens");
    }
}
