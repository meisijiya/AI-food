package com.ai.food.mapper;

import com.ai.food.model.ConversationSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 对话会话 Mapper（MyBatis-Plus 迁移版）
 * <p>
 * 由原 JPA {@code ConversationSessionRepository} 翻译而来。
 * 表名 conversation_session；软删过滤（{@code is_deleted = 0}）由调用方显式写在 SQL 中，
 * 不依赖 {@code @TableLogic} 自动注入（其只作用于 BaseMapper 内置方法）。
 */
@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSession> {

    /**
     * 按 sessionId 查找会话（String 业务键，非主键）
     */
    @Select("SELECT * FROM conversation_session WHERE session_id = #{sessionId} AND is_deleted = 0 LIMIT 1")
    ConversationSession findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 判断 sessionId 是否存在
     */
    @Select("SELECT COUNT(*) FROM conversation_session WHERE session_id = #{sessionId} AND is_deleted = 0")
    Long existsBySessionId(@Param("sessionId") String sessionId);

    /**
     * 按 userId 分页查询会话（按创建时间倒序）
     */
    @Select("SELECT * FROM conversation_session WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    IPage<ConversationSession> selectByUserIdOrderByCreatedAtDesc(IPage<ConversationSession> page, @Param("userId") Long userId);

    /**
     * 按 userId 分页查询会话（按创建时间正序）
     */
    @Select("SELECT * FROM conversation_session WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at ASC")
    IPage<ConversationSession> selectByUserIdOrderByCreatedAtAsc(IPage<ConversationSession> page, @Param("userId") Long userId);

    /**
     * 软删除：标记 is_deleted=1 + 写 deleted_at + 乐观锁 version+1
     */
    @Update("UPDATE conversation_session SET is_deleted = 1, deleted_at = CURRENT_TIMESTAMP, version = version + 1 " +
            "WHERE session_id = #{sessionId}")
    int softDeleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 批量软删除（IN 子句用 foreach 展开 List 参数）
     */
    @Update({
            "<script>",
            "UPDATE conversation_session SET is_deleted = 1, deleted_at = CURRENT_TIMESTAMP, version = version + 1 ",
            "WHERE session_id IN ",
            "<foreach collection='sessionIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int softDeleteBySessionIdIn(@Param("sessionIds") List<String> sessionIds);

    /**
     * 物理删除已软删的会话（带 WHERE 不触发 BlockAttackInnerInterceptor）
     */
    @Delete("DELETE FROM conversation_session WHERE is_deleted = 1")
    int hardDeleteAllSoftDeleted();

    /**
     * 取用户最近 10 条会话（按创建时间倒序）
     */
    @Select("SELECT * FROM conversation_session WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC LIMIT 10")
    List<ConversationSession> findTop10ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}