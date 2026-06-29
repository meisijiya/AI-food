package com.ai.food.mapper;

import com.ai.food.model.QaRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * QA 问答记录 Mapper（MyBatis-Plus 迁移版）
 * <p>
 * 由原 JPA {@code QaRecordRepository} 翻译而来。
 * <p>
 * 注意：实体 {@code isValid} 是业务字段（非软删除），{@code isDeleted} 才是软删除字段，
 * SQL 中 WHERE 仅过滤 {@code is_deleted = 0}。
 */
@Mapper
public interface QaRecordMapper extends BaseMapper<QaRecord> {

    /**
     * 按 sessionId 列出问答记录（按 question_order 升序）
     */
    @Select("SELECT * FROM qa_record WHERE session_id = #{sessionId} AND is_deleted = 0 ORDER BY question_order ASC")
    List<QaRecord> findBySessionIdOrderByQuestionOrderAsc(@Param("sessionId") String sessionId);

    /**
     * 按 sessionId 列出问答记录（无排序）
     */
    @Select("SELECT * FROM qa_record WHERE session_id = #{sessionId} AND is_deleted = 0")
    List<QaRecord> findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 统计某会话问答记录总数
     */
    @Select("SELECT COUNT(*) FROM qa_record WHERE session_id = #{sessionId} AND is_deleted = 0")
    Long countBySessionId(@Param("sessionId") String sessionId);

    /**
     * 软删除：标记 is_deleted=1 + 乐观锁 version+1
     */
    @Update("UPDATE qa_record SET is_deleted = 1, version = version + 1 WHERE session_id = #{sessionId}")
    int softDeleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 批量软删除（IN 子句用 foreach 展开 List 参数）
     */
    @Update({
            "<script>",
            "UPDATE qa_record SET is_deleted = 1, version = version + 1 ",
            "WHERE session_id IN ",
            "<foreach collection='sessionIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int softDeleteBySessionIdIn(@Param("sessionIds") List<String> sessionIds);

    /**
     * 物理删除已软删的问答记录（带 WHERE 不触发 BlockAttackInnerInterceptor）
     */
    @Delete("DELETE FROM qa_record WHERE is_deleted = 1")
    int hardDeleteAllSoftDeleted();
}
