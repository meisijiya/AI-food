package com.ai.food.mapper;

import com.ai.food.model.Photo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Photo 实体的 MyBatis-Plus Mapper。
 * <p>由 PhotoRepository（JPA）翻译而来，JPQL 中的实体名与驼峰字段映射到对应的下划线表/列。
 * <p>注意：{@code @TableLogic} 仅对 {@link BaseMapper} 自动生成的 select/delete 生效，
 * 自定义 {@code @Select} 注解 SQL 不会自动追加 {@code WHERE is_deleted = 0}，
 * 因此每个查询语句均显式附加软删过滤条件。
 */
@Mapper
public interface PhotoMapper extends BaseMapper<Photo> {

    /**
     * 查询指定用户的所有照片，按 created_at 倒序。
     */
    @Select("SELECT * FROM photo WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<Photo> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 取指定 session 最新一条照片（一个 session 可能上传多次）。
     */
    @Select("SELECT * FROM photo WHERE related_session_id = #{sessionId} AND is_deleted = 0 " +
            "ORDER BY created_at DESC LIMIT 1")
    Photo findFirstByRelatedSessionIdOrderByCreatedAtDesc(@Param("sessionId") String sessionId);

    /**
     * 批量查询多个 session 的全部照片，按 created_at 倒序。
     */
    @Select("<script>SELECT * FROM photo WHERE is_deleted = 0 AND related_session_id IN " +
            "<foreach collection='sessionIds' item='sid' open='(' separator=',' close=')'>" +
            "#{sid}</foreach> " +
            "ORDER BY created_at DESC</script>")
    List<Photo> findByRelatedSessionIdInOrderByCreatedAtDesc(@Param("sessionIds") List<String> sessionIds);

    /**
     * 按 userId + relatedSessionId 查询（可能多条，故返回 List）。
     */
    @Select("SELECT * FROM photo WHERE user_id = #{userId} AND related_session_id = #{sessionId} AND is_deleted = 0")
    List<Photo> findByUserIdAndRelatedSessionId(@Param("userId") Long userId,
                                                 @Param("sessionId") String sessionId);

    /**
     * 按 userId + originalPath 查询唯一一条；调用方按需包装 {@code Optional}。
     */
    @Select("SELECT * FROM photo WHERE user_id = #{userId} AND original_path = #{originalPath} " +
            "AND is_deleted = 0 LIMIT 1")
    Photo findByUserIdAndOriginalPath(@Param("userId") Long userId,
                                      @Param("originalPath") String originalPath);

    /**
     * 按 thumbnailPath 查询唯一一条。
     */
    @Select("SELECT * FROM photo WHERE thumbnail_path = #{thumbnailPath} AND is_deleted = 0 LIMIT 1")
    Photo findByThumbnailPath(@Param("thumbnailPath") String thumbnailPath);

    /**
     * 查询所有已逻辑删除的照片（{@code is_deleted = 1}）。
     */
    @Select("SELECT * FROM photo WHERE is_deleted = 1")
    List<Photo> findAllByIsDeletedTrue();

    /**
     * 物理删除所有已逻辑删除的记录，返回受影响行数。
     */
    @Delete("DELETE FROM photo WHERE is_deleted = 1")
    int hardDeleteAllSoftDeleted();
}