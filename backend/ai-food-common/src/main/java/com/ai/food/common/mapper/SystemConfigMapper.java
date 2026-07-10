package com.ai.food.common.mapper;

import com.ai.food.common.model.SystemConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    @Select("SELECT * FROM system_config WHERE config_key = #{configKey} LIMIT 1")
    SystemConfig findByKey(@Param("configKey") String configKey);

    @Update("UPDATE system_config SET config_value = #{configValue}, version = version + 1 WHERE config_key = #{configKey}")
    int updateByKey(@Param("configKey") String configKey, @Param("configValue") String configValue);
}
