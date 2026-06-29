package com.ai.food.common.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 调用结果包装：文本 + token 用量 + 模型名。
 * <p>
 * 之前 {@code chat()} 只返回 {@code String}，token 用量被丢弃，无法在 {@code qa_record}
 * 中记账。本类用于把 {@code ChatResponse.getMetadata().getUsage()} 一起带回给调用方。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResult {

    /** 模型生成的文本 */
    private String text;

    /** prompt token 数（无 usage 时为 null） */
    private Long promptTokens;

    /** completion token 数（无 usage 时为 null） */
    private Long completionTokens;

    /** 总 token 数（无 usage 时为 null） */
    private Long totalTokens;

    /** 调用的模型名（无 metadata 时为 null） */
    private String model;

    /**
     * 仅文本的便捷工厂方法——失败兜底时使用。
     */
    public static ChatResult of(String text) {
        return new ChatResult(text, null, null, null, null);
    }
}
