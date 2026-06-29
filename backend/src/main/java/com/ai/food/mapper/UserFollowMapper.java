package com.ai.food.mapper;

import com.ai.food.model.UserFollow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户关注关系表 MyBatis-Plus Mapper 接口。
 *
 * <p>由原 JPA {@code UserFollowRepository} 翻译而来，
 * 提供关注/粉丝关系查询、计数与取消关注等能力。
 *
 * <p>注意：{@code deleteByFollowerIdAndFollowingId} 翻译为硬删（{@code @Delete}），
 * 以严格保留原 JPA 派生删除方法语义（原实体未标注 {@code @SQLDelete}，JPA 默认硬删）。
 * 如需改为软删，由 Service 层在后续 Wave 中决策。
 */
// ponytail: 派生方法翻译为 @Select 注解而非 LambdaQueryWrapper，便于 SQL 审计与索引观察
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /**
     * 按 (followerId, followingId) 唯一定位一条关注关系。
     *
     * @param followerId  关注者 id
     * @param followingId 被关注者 id
     * @return 匹配的关注关系实体，未命中返回 null
     */
    @Select("SELECT * FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId} AND is_deleted = 0 LIMIT 1")
    UserFollow findByFollowerIdAndFollowingId(@Param("followerId") Long followerId,
                                             @Param("followingId") Long followingId);

    /**
     * 查询某用户关注的所有记录（即 "我的关注列表"）。
     *
     * @param followerId 关注者 id
     * @return 该用户作为关注者的所有关注关系
     */
    @Select("SELECT * FROM user_follow WHERE follower_id = #{followerId} AND is_deleted = 0")
    List<UserFollow> findByFollowerId(@Param("followerId") Long followerId);

    /**
     * 查询关注某用户的所有记录（即 "我的粉丝列表"）。
     *
     * @param followingId 被关注者 id
     * @return 关注该用户的所有关注关系
     */
    @Select("SELECT * FROM user_follow WHERE following_id = #{followingId} AND is_deleted = 0")
    List<UserFollow> findByFollowingId(@Param("followingId") Long followingId);

    /**
     * 统计某用户关注的总人数。
     *
     * @param followerId 关注者 id
     * @return 关注数（仅统计未软删记录）
     */
    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{followerId} AND is_deleted = 0")
    Long countByFollowerId(@Param("followerId") Long followerId);

    /**
     * 统计某用户的粉丝总人数。
     *
     * @param followingId 被关注者 id
     * @return 粉丝数（仅统计未软删记录）
     */
    @Select("SELECT COUNT(*) FROM user_follow WHERE following_id = #{followingId} AND is_deleted = 0")
    Long countByFollowingId(@Param("followingId") Long followingId);

    /**
     * 取消关注：硬删指定 (followerId, followingId) 关系。
     *
     * <p>采用硬删以保留原 JPA 派生删除语义；如需软删可改写为
     * {@code @Update("UPDATE user_follow SET is_deleted = 1, version = version + 1 WHERE ...")}。
     *
     * @param followerId  关注者 id
     * @param followingId 被关注者 id
     * @return 受影响行数
     */
    @Delete("DELETE FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int deleteByFollowerIdAndFollowingId(@Param("followerId") Long followerId,
                                         @Param("followingId") Long followingId);

    /**
     * 判断 (followerId, followingId) 关系是否存在。
     *
     * @param followerId  关注者 id
     * @param followingId 被关注者 id
     * @return true 存在；false 不存在
     */
    @Select("SELECT EXISTS(SELECT 1 FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId} AND is_deleted = 0)")
    Boolean existsByFollowerIdAndFollowingId(@Param("followerId") Long followerId,
                                             @Param("followingId") Long followingId);

    /**
     * 判断两个用户是否互相关注（双方都关注对方即为互关）。
     *
     * <p>SQL 等价于：存在两条记录，一条 (id1→id2)，另一条 (id2→id1)。
     *
     * @param id1 用户 A 的 id
     * @param id2 用户 B 的 id
     * @return true 互关；false 非互关
     */
    @Select("SELECT (SELECT COUNT(*) FROM user_follow WHERE (follower_id = #{id1} AND following_id = #{id2}) AND is_deleted = 0) "
            + "+ (SELECT COUNT(*) FROM user_follow WHERE (follower_id = #{id2} AND following_id = #{id1}) AND is_deleted = 0) = 2")
    Boolean isMutualFollow(@Param("id1") Long id1, @Param("id2") Long id2);

    /**
     * 查询某用户关注的所有目标用户 id 列表。
     *
     * @param userId 用户 id（作为关注者）
     * @return 被关注用户 id 集合
     */
    @Select("SELECT following_id FROM user_follow WHERE follower_id = #{userId} AND is_deleted = 0")
    List<Long> findFollowingIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询某用户的所有粉丝 id 列表。
     *
     * @param userId 用户 id（作为被关注者）
     * @return 粉丝用户 id 集合
     */
    @Select("SELECT follower_id FROM user_follow WHERE following_id = #{userId} AND is_deleted = 0")
    List<Long> findFollowerIdsByUserId(@Param("userId") Long userId);
}
