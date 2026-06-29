package com.ai.food.common.mapper;

import com.ai.food.common.model.FeedComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * FeedComment 实体的 MyBatis-Plus Mapper。
 * <p>由 FeedCommentRepository（JPA）翻译而来。
 * <p>{@code @TableLogic} 仅对 {@link BaseMapper} 自动方法生效，自定义注解 SQL 不会自动
 * 追加 {@code WHERE is_deleted = 0}，故每个查询显式附加软删过滤条件。
 * <p>{@code @Update} 不会自动触发乐观锁条件，故显式递增 {@code version}。
 */
@Mapper
public interface FeedCommentMapper extends BaseMapper<FeedComment> {

    /**
     * 分页：按 postId 查询某帖的全部评论，按 created_at 倒序。
     */
    @Select("SELECT * FROM feed_comment WHERE post_id = #{postId} AND is_deleted = 0 ORDER BY created_at DESC")
    IPage<FeedComment> findByPostIdOrderByCreatedAtDesc(IPage<FeedComment> page,
                                                       @Param("postId") Long postId);

    /**
     * 按 id + userId 查找唯一一条（用于校验评论归属再删除）。
     */
    @Select("SELECT * FROM feed_comment WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0 LIMIT 1")
    FeedComment findByIdAndUserId(@Param("id") Long id,
                                  @Param("userId") Long userId);

    /**
     * 查询全部已逻辑删除的评论（{@code is_deleted = 1}）。
     */
    @Select("SELECT * FROM feed_comment WHERE is_deleted = 1")
    List<FeedComment> findAllByIsDeletedTrue();

    /**
     * 统计某 postId 下未删除评论数量。
     */
    @Select("SELECT COUNT(*) FROM feed_comment WHERE post_id = #{postId} AND is_deleted = 0")
    long countByPostId(@Param("postId") Long postId);

    /**
     * 软删除：校验归属（id + userId 同时匹配）才删除，返回受影响行数。
     */
    @Update("UPDATE feed_comment SET is_deleted = 1, version = version + 1 " +
            "WHERE id = #{commentId} AND user_id = #{userId}")
    int softDeleteByIdAndUserId(@Param("commentId") Long commentId,
                                @Param("userId") Long userId);

    /**
     * 软删除：按 postId（帖子级联删除评论场景）。
     */
    @Update("UPDATE feed_comment SET is_deleted = 1, version = version + 1 WHERE post_id = #{postId}")
    int softDeleteByPostId(@Param("postId") Long postId);

    /**
     * 软删除：按 postId 集合。
     */
    @Update("<script>UPDATE feed_comment SET is_deleted = 1, version = version + 1 WHERE post_id IN " +
            "<foreach collection='postIds' item='pid' open='(' separator=',' close=')'>" +
            "#{pid}</foreach></script>")
    int softDeleteByPostIdIn(@Param("postIds") List<Long> postIds);

    /**
     * 物理删除全部已逻辑删除的记录，返回受影响行数。
     */
    @Delete("DELETE FROM feed_comment WHERE is_deleted = 1")
    int hardDeleteAllSoftDeleted();
}
