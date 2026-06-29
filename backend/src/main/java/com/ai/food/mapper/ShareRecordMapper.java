package com.ai.food.mapper;

import com.ai.food.model.ShareRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * ShareRecord 实体的 MyBatis-Plus Mapper。
 * <p>由 ShareRecordRepository（JPA）翻译而来。仅含两个唯一定位查询，其余 CRUD
 * 直接由 {@link BaseMapper} 提供。
 * <p>{@code @TableLogic} 仅对 {@link BaseMapper} 自动方法生效，自定义注解 SQL
 * 不会自动追加 {@code WHERE is_deleted = 0}，故查询显式附加软删过滤条件。
 */
@Mapper
public interface ShareRecordMapper extends BaseMapper<ShareRecord> {

    /**
     * 按 share_token 查找唯一一条（分享链接打开场景的主键定位）。
     */
    @Select("SELECT * FROM share_record WHERE share_token = #{shareToken} AND is_deleted = 0 LIMIT 1")
    ShareRecord findByShareToken(@Param("shareToken") String shareToken);

    /**
     * 按 sessionId + userId 查找唯一一条。
     */
    @Select("SELECT * FROM share_record WHERE session_id = #{sessionId} AND user_id = #{userId} " +
            "AND is_deleted = 0 LIMIT 1")
    ShareRecord findBySessionIdAndUserId(@Param("sessionId") String sessionId,
                                         @Param("userId") Long userId);
}
