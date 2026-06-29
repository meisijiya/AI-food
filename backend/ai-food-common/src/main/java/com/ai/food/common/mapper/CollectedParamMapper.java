package com.ai.food.common.mapper;

import com.ai.food.common.model.CollectedParam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 已收集参数 Mapper（MyBatis-Plus 迁移版）
 * <p>
 * 由原 JPA {@code CollectedParamRepository} 翻译而来。
 * <p>
 * 注意：表名 {@code collected_params}（复数），与 {@code user_bloom_filter} 等单数表区分；
 * 唯一约束 (session_id, param_name) 由 Flyway 迁移脚本管理。
 */
@Mapper
public interface CollectedParamMapper extends BaseMapper<CollectedParam> {

    /**
     * 按 sessionId 列出所有已收集参数
     */
    @Select("SELECT * FROM collected_params WHERE session_id = #{sessionId} AND is_deleted = 0")
    List<CollectedParam> findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 按 (sessionId, paramName) 查找单条参数
     */
    @Select("SELECT * FROM collected_params WHERE session_id = #{sessionId} AND param_name = #{paramName} AND is_deleted = 0 LIMIT 1")
    CollectedParam findBySessionIdAndParamName(@Param("sessionId") String sessionId, @Param("paramName") String paramName);

    /**
     * 判断 (sessionId, paramName) 是否存在
     */
    @Select("SELECT COUNT(*) FROM collected_params WHERE session_id = #{sessionId} AND param_name = #{paramName} AND is_deleted = 0")
    Long existsBySessionIdAndParamName(@Param("sessionId") String sessionId, @Param("paramName") String paramName);

    /**
     * 统计某会话已收集参数总数
     */
    @Select("SELECT COUNT(*) FROM collected_params WHERE session_id = #{sessionId} AND is_deleted = 0")
    Long countBySessionId(@Param("sessionId") String sessionId);

    /**
     * 软删除：标记 is_deleted=1 + 乐观锁 version+1
     */
    @Update("UPDATE collected_params SET is_deleted = 1, version = version + 1 WHERE session_id = #{sessionId}")
    int softDeleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 批量软删除（IN 子句用 foreach 展开 List 参数）
     */
    @Update({
            "<script>",
            "UPDATE collected_params SET is_deleted = 1, version = version + 1 ",
            "WHERE session_id IN ",
            "<foreach collection='sessionIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int softDeleteBySessionIdIn(@Param("sessionIds") List<String> sessionIds);

    /**
     * 物理删除已软删的参数（带 WHERE 不触发 BlockAttackInnerInterceptor）
     */
    @Delete("DELETE FROM collected_params WHERE is_deleted = 1")
    int hardDeleteAllSoftDeleted();
}
