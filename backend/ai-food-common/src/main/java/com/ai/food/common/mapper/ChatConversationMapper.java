package com.ai.food.common.mapper;

import com.ai.food.common.model.ChatConversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话 Mapper（MyBatis-Plus）。
 * <p>
 * 由原 JPA {@code ChatConversationRepository} 翻译而来，对应表 {@code chat_conversation}。
 * 该表无 {@code is_deleted} 列，故不依赖 @TableLogic；{@code @Version} 乐观锁仍然启用。
 * </p>
 */
@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    /**
     * 按会话 key 查找会话
     */
    @Select("SELECT * FROM chat_conversation WHERE conversation_key = #{conversationKey} LIMIT 1")
    ChatConversation findByConversationKey(@Param("conversationKey") String conversationKey);

    /**
     * 获取用户的聊天列表，排除已隐藏且无新消息的会话
     */
    @Select("SELECT * FROM chat_conversation " +
           "WHERE (user1_id = #{userId} OR user2_id = #{userId}) " +
           "  AND ( (#{userId} = user1_id AND (hidden_at_user1 IS NULL OR last_message_at > hidden_at_user1)) " +
           "     OR (#{userId} = user2_id AND (hidden_at_user2 IS NULL OR last_message_at > hidden_at_user2)) ) " +
           "ORDER BY last_message_at DESC")
    List<ChatConversation> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId);

    /**
     * 设置 user1 已清除并隐藏该会话
     */
    @Update("UPDATE chat_conversation " +
            "SET cleared_at_user1 = #{clearedAt}, hidden_at_user1 = #{clearedAt}, version = version + 1 " +
            "WHERE id = #{id}")
    int setClearedAndHiddenAtUser1(@Param("id") Long id, @Param("clearedAt") LocalDateTime clearedAt);

    /**
     * 设置 user2 已清除并隐藏该会话
     */
    @Update("UPDATE chat_conversation " +
            "SET cleared_at_user2 = #{clearedAt}, hidden_at_user2 = #{clearedAt}, version = version + 1 " +
            "WHERE id = #{id}")
    int setClearedAndHiddenAtUser2(@Param("id") Long id, @Param("clearedAt") LocalDateTime clearedAt);

    /**
     * 重置 user1 的隐藏时间戳（新消息到来时会话重新出现）
     */
    @Update("UPDATE chat_conversation SET hidden_at_user1 = NULL, version = version + 1 WHERE id = #{id}")
    int resetHiddenAtUser1(@Param("id") Long id);

    /**
     * 重置 user2 的隐藏时间戳
     */
    @Update("UPDATE chat_conversation SET hidden_at_user2 = NULL, version = version + 1 WHERE id = #{id}")
    int resetHiddenAtUser2(@Param("id") Long id);

    /**
     * 更新会话最后一条消息摘要及时间
     */
    @Update("UPDATE chat_conversation " +
            "SET last_message = #{lastMessage}, last_message_at = #{lastMessageAt}, version = version + 1 " +
            "WHERE id = #{id}")
    int updateLastMessage(@Param("id") Long id,
                          @Param("lastMessage") String lastMessage,
                          @Param("lastMessageAt") LocalDateTime lastMessageAt);

    /**
     * 查找双方都已清除的会话（可用于硬删除清理）
     */
    @Select("SELECT * FROM chat_conversation WHERE cleared_at_user1 IS NOT NULL AND cleared_at_user2 IS NOT NULL")
    List<ChatConversation> findAllBothCleared();
}
