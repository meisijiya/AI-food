package com.ai.food.service.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 对话模块共享常量（Redis key + ObjectMapper + 必选/可选参数列表）。
 * <p>
 * 提取自原 {@code ConversationService}（618 行），按 fix-7 / fix-9 同模式：常量集中避免各 service
 * 重复硬编码字符串与魔法数字。
 * </p>
 *
 * <p>ponytail: 纯常量类，0 业务逻辑。</p>
 */
public final class ConversationUtil {

    private ConversationUtil() {
        // utility class
    }

    /** 共享 ObjectMapper（用于解析 AI 返回的 JSON）。 */
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 推荐结果待处理标记 key（Redis String，TTL 7 天）：{@code pending:recommend:{userId}}。 */
    public static final String PENDING_RECOMMEND_KEY = "pending:recommend:";

    /** 必选参数数量。 */
    public static final int REQUIRED_PARAMS_COUNT = 7;

    /** 必选参数列表（顺序敏感，{@code determineNextParam} 按此顺序推进）。 */
    public static final List<String> REQUIRED_PARAMS = List.of(
            "time", "location", "weather", "mood", "companion", "budget", "taste"
    );

    /** 可选参数列表（必选收齐后由 AI 自由对话阶段按此顺序补问）。 */
    public static final List<String> OPTIONAL_PARAMS = List.of(
            "restriction", "preference", "health"
    );
}
