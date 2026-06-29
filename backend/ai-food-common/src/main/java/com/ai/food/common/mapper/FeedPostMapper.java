package com.ai.food.common.mapper;

import com.ai.food.common.model.FeedPost;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * FeedPost 实体的 MyBatis-Plus Mapper。
 * <p>由 FeedPostRepository（JPA）翻译而来。
 * <p>分页查询一律使用 {@link IPage} 入参，不再写 {@code LIMIT} —— 由
 * {@code PaginationInnerInterceptor} 自动拼装。
 * <p>{@code @TableLogic} 仅对 {@link BaseMapper} 自动方法生效，自定义注解 SQL
 * 不会自动追加 {@code WHERE is_deleted = 0}，故每个查询显式附加软删过滤条件。
 * <p>{@code @Update} 不会自动触发乐观锁条件，故显式递增 {@code version}。
 */
@Mapper
public interface FeedPostMapper extends BaseMapper<FeedPost> {

    /**
     * 按 sessionId + userId 查找唯一一条。
     */
    @Select("SELECT * FROM feed_post WHERE session_id = #{sessionId} AND user_id = #{userId} " +
            "AND is_deleted = 0 LIMIT 1")
    FeedPost findBySessionIdAndUserId(@Param("sessionId") String sessionId,
                                      @Param("userId") Long userId);

    /**
     * 分页：全量按 published_at 倒序。
     */
    @Select("SELECT * FROM feed_post WHERE is_deleted = 0 ORDER BY published_at DESC")
    IPage<FeedPost> findByOrderByPublishedAtDesc(IPage<FeedPost> page);

    /**
     * 分页：按 food_name 模糊匹配。
     */
    @Select("SELECT * FROM feed_post WHERE food_name LIKE CONCAT('%', #{foodName}, '%') " +
            "AND is_deleted = 0 ORDER BY published_at DESC")
    IPage<FeedPost> findByFoodNameContainingOrderByPublishedAtDesc(IPage<FeedPost> page,
                                                                   @Param("foodName") String foodName);

    /**
     * 分页：按 user_id 查询某人全部 FeedPost。
     */
    @Select("SELECT * FROM feed_post WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY published_at DESC")
    IPage<FeedPost> findByUserIdOrderByPublishedAtDesc(IPage<FeedPost> page,
                                                       @Param("userId") Long userId);

    /**
     * 分页：foodName / paramValue 均可空，空值跳过对应 LIKE 条件；按 published_at 倒序。
     * <p>对应原 JPQL：{@code WHERE (:foodName IS NULL OR foodName LIKE %:foodName%) AND
     * (:paramValue IS NULL OR collectedParams LIKE %:paramValue%) ORDER BY publishedAt DESC}。
     */
    @Select("<script>SELECT * FROM feed_post " +
            "<where>" +
            "<if test='foodName != null'>AND food_name LIKE CONCAT('%', #{foodName}, '%')</if>" +
            "<if test='paramValue != null'>AND collected_params LIKE CONCAT('%', #{paramValue}, '%')</if>" +
            "AND is_deleted = 0 " +
            "</where> " +
            "ORDER BY published_at DESC</script>")
    IPage<FeedPost> findByFilters(IPage<FeedPost> page,
                                  @Param("foodName") String foodName,
                                  @Param("paramValue") String paramValue);

    /**
     * 分页：仅 {@code visibility = 'public'}，foodName / paramValue 可空。
     */
    @Select("<script>SELECT * FROM feed_post WHERE visibility = 'public' " +
            "<if test='foodName != null'>AND food_name LIKE CONCAT('%', #{foodName}, '%')</if>" +
            "<if test='paramValue != null'>AND collected_params LIKE CONCAT('%', #{paramValue}, '%')</if>" +
            "AND is_deleted = 0 " +
            "ORDER BY published_at DESC</script>")
    IPage<FeedPost> findPublicByFilters(IPage<FeedPost> page,
                                       @Param("foodName") String foodName,
                                       @Param("paramValue") String paramValue);

    /**
     * 分页：{@code public} 全部可见 + {@code friends} 仅粉丝可见（userId IN followingIds）。
     * <p>对应原 JPQL：{@code ((visibility='public') OR (visibility='friends' AND userId IN :followingIds)) AND ...}。
     */
    @Select("<script>SELECT * FROM feed_post WHERE " +
            "((visibility = 'public') OR (visibility = 'friends' AND user_id IN " +
            "<foreach collection='followingIds' item='uid' open='(' separator=',' close=')'>" +
            "#{uid}</foreach>)) " +
            "<if test='foodName != null'>AND food_name LIKE CONCAT('%', #{foodName}, '%')</if>" +
            "<if test='paramValue != null'>AND collected_params LIKE CONCAT('%', #{paramValue}, '%')</if>" +
            "AND is_deleted = 0 " +
            "ORDER BY published_at DESC</script>")
    IPage<FeedPost> findPublicAndFansOnlyByFilters(IPage<FeedPost> page,
                                                   @Param("followingIds") List<Long> followingIds,
                                                   @Param("foodName") String foodName,
                                                   @Param("paramValue") String paramValue);

    /**
     * 分页：按 userId 集合查询（关注流场景）。
     */
    @Select("<script>SELECT * FROM feed_post WHERE is_deleted = 0 AND user_id IN " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>" +
            "#{uid}</foreach> " +
            "ORDER BY published_at DESC</script>")
    IPage<FeedPost> findByUserIdsOrderByPublishedAtDesc(IPage<FeedPost> page,
                                                        @Param("userIds") List<Long> userIds);

    /**
     * 按 id 集合批量查询（不分页，用于评论统计/关联补齐等）。
     */
    @Select("<script>SELECT * FROM feed_post WHERE is_deleted = 0 AND id IN " +
            "<foreach collection='ids' item='iid' open='(' separator=',' close=')'>" +
            "#{iid}</foreach></script>")
    List<FeedPost> findByIdIn(@Param("ids") List<Long> ids);

    /**
     * 软删除：按 sessionId。显式递增 {@code version} 以与乐观锁语义对齐。
     */
    @Update("UPDATE feed_post SET is_deleted = 1, version = version + 1 WHERE session_id = #{sessionId}")
    int softDeleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 软删除：按 sessionId 集合。
     */
    @Update("<script>UPDATE feed_post SET is_deleted = 1, version = version + 1 WHERE session_id IN " +
            "<foreach collection='sessionIds' item='sid' open='(' separator=',' close=')'>" +
            "#{sid}</foreach></script>")
    int softDeleteBySessionIdIn(@Param("sessionIds") List<String> sessionIds);

    /**
     * 软删除：按 postId。
     */
    @Update("UPDATE feed_post SET is_deleted = 1, version = version + 1 WHERE id = #{postId}")
    int softDeleteByPostId(@Param("postId") Long postId);

    /**
     * 软删除：按 postId 集合。
     */
    @Update("<script>UPDATE feed_post SET is_deleted = 1, version = version + 1 WHERE id IN " +
            "<foreach collection='postIds' item='pid' open='(' separator=',' close=')'>" +
            "#{pid}</foreach></script>")
    int softDeleteByPostIdIn(@Param("postIds") List<Long> postIds);

    /**
     * 按 sessionId 查找唯一未删除的 FeedPost（JPQL 中 {@code WHERE isDeleted = false} 已翻译为
     * {@code AND is_deleted = 0}）。
     */
    @Select("SELECT * FROM feed_post WHERE session_id = #{sessionId} AND is_deleted = 0 LIMIT 1")
    FeedPost findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 按 sessionId 集合批量查找未删除的 FeedPost。
     */
    @Select("<script>SELECT * FROM feed_post WHERE is_deleted = 0 AND session_id IN " +
            "<foreach collection='sessionIds' item='sid' open='(' separator=',' close=')'>" +
            "#{sid}</foreach></script>")
    List<FeedPost> findBySessionIdIn(@Param("sessionIds") List<String> sessionIds);

    /**
     * 物理删除全部已逻辑删除的记录，返回受影响行数。
     */
    @Delete("DELETE FROM feed_post WHERE is_deleted = 1")
    int hardDeleteAllSoftDeleted();

    /**
     * 查询全部已逻辑删除的记录（{@code is_deleted = 1}）。
     */
    @Select("SELECT * FROM feed_post WHERE is_deleted = 1")
    List<FeedPost> findAllByIsDeletedTrue();
}
