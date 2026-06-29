package com.ai.food.mapper;

import com.ai.food.model.BloomSyncLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 布隆同步日志表 MyBatis-Plus Mapper 接口。
 *
 * <p>由原 JPA {@code BloomSyncLogRepository} 翻译而来，
 * 用于记录布隆过滤器重建/同步任务的状态、错误信息及时间戳。
 */
// ponytail: 派生方法翻译为 @Select 注解而非 LambdaQueryWrapper，便于 SQL 审计与索引观察
@Mapper
public interface BloomSyncLogMapper extends BaseMapper<BloomSyncLog> {

    /**
     * 查询某用户的所有同步日志。
     *
     * @param userId 用户 id
     * @return 该用户的同步日志列表
     */
    @Select("SELECT * FROM bloom_sync_log WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY synced_at DESC")
    List<BloomSyncLog> findByUserId(@Param("userId") Long userId);

    /**
     * 查询指定时间点之后的所有同步日志。
     *
     * @param after 起始时间（{@code synced_at > after}）
     * @return 时间区间内的同步日志列表
     */
    @Select("SELECT * FROM bloom_sync_log WHERE synced_at > #{after} AND is_deleted = 0 ORDER BY synced_at DESC")
    List<BloomSyncLog> findBySyncedAtAfter(@Param("after") LocalDateTime after);

    /**
     * 按状态统计同步日志条数。
     *
     * @param status 同步状态字符串（如 SUCCESS / FAILED / RUNNING）
     * @return 对应状态的日志总数（仅统计未软删记录）
     */
    @Select("SELECT COUNT(*) FROM bloom_sync_log WHERE status = #{status} AND is_deleted = 0")
    Long countByStatus(@Param("status") String status);
}
