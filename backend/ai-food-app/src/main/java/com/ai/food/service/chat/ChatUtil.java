package com.ai.food.service.chat;

/**
 * 聊天模块 Redis key 常量 + 业务阈值常量。
 * <p>
 * 提取自原 {@code ChatService}（752 行），按 fix-7 FeedService 拆分同模式：常量集中避免各 service
 * 重复硬编码字符串与魔法数字。
 * </p>
 *
 * <p>ponytail: 纯常量类，0 业务逻辑。任何 Redis key 拼装或阈值变更都只动这里。</p>
 */
public final class ChatUtil {

    private ChatUtil() {
        // utility class
    }

    /** 用户各会话未读计数（Redis Hash，field = conversationId）。 */
    public static final String UNREAD_KEY = "chat:unread:";

    /** 用户全局未读总数（Redis String）。 */
    public static final String UNREAD_TOTAL_KEY = "chat:unread:total:";

    /** 用户在线标记（Redis String，5 分钟 TTL）。 */
    public static final String ONLINE_KEY = "chat:online:";

    /** 非互关消息计数（Redis String，senderId:receiverId）。 */
    public static final String MSG_COUNT_KEY = "chat:msgcount:";

    /** 非互关最大可发送消息数。 */
    public static final int MAX_NON_MUTUAL_MESSAGES = 5;
}
