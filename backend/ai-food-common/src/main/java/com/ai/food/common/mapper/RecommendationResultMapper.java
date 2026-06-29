package com.ai.food.common.mapper;

import com.ai.food.common.model.RecommendationResult;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 推荐结果 Mapper（MyBatis-Plus 迁移版）
 * <p>
 * 由原 JPA {@code RecommendationResultRepository} 翻译而来。
 */
@Mapper
public interface RecommendationResultMapper extends BaseMapper<RecommendationResult> {

    /**
     * 按 sessionId 查找推荐结果
     */
    @Select("SELECT * FROM recommendation_result WHERE session_id = #{sessionId} AND is_deleted = 0 LIMIT 1")
    RecommendationResult findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 批量按 sessionId 查找推荐结果（IN 子句用 foreach 展开 List 参数）
     */
    @Select({
            "<script>",
            "SELECT * FROM recommendation_result WHERE session_id IN ",
            "<foreach collection='sessionIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            " AND is_deleted = 0",
            "</script>"
    })
    List<RecommendationResult> findBySessionIdIn(@Param("sessionIds") List<String> sessionIds);

    /**
     * 软删除：标记 is_deleted=1 + 乐观锁 version+1
     */
    @Update("UPDATE recommendation_result SET is_deleted = 1, version = version + 1 WHERE session_id = #{sessionId}")
    int softDeleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 批量软删除（IN 子句用 foreach 展开 List 参数）
     */
    @Update({
            "<script>",
            "UPDATE recommendation_result SET is_deleted = 1, version = version + 1 ",
            "WHERE session_id IN ",
            "<foreach collection='sessionIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int softDeleteBySessionIdIn(@Param("sessionIds") List<String> sessionIds);

    /**
     * 物理删除已软删的推荐结果（带 WHERE 不触发 BlockAttackInnerInterceptor）
     */
    @Delete("DELETE FROM recommendation_result WHERE is_deleted = 1")
    int hardDeleteAllSoftDeleted();
}
