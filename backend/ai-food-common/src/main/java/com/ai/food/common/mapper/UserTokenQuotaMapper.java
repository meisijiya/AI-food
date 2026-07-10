package com.ai.food.common.mapper;

import com.ai.food.common.model.UserTokenQuota;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserTokenQuotaMapper extends BaseMapper<UserTokenQuota> {

    @Select("SELECT * FROM user_token_quota WHERE user_id = #{userId} LIMIT 1")
    UserTokenQuota findByUserId(@Param("userId") Long userId);
}
