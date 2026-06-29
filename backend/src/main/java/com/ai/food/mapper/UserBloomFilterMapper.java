package com.ai.food.mapper;

import com.ai.food.model.UserBloomFilter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户布隆过滤器表 MyBatis-Plus Mapper 接口。
 *
 * <p>由原 JPA {@code UserBloomFilterRepository} 翻译而来，
 * 用于管理每个用户对应的布隆过滤器位数组（{@code bit_array} BLOB）。
 */
// ponytail: 派生方法翻译为 @Select 注解而非 LambdaQueryWrapper，便于 SQL 审计与索引观察
@Mapper
public interface UserBloomFilterMapper extends BaseMapper<UserBloomFilter> {

    /**
     * 按 userId 查询布隆过滤器记录。
     *
     * @param userId 用户 id
     * @return 对应用户的布隆过滤器实体，未命中返回 null
     */
    @Select("SELECT * FROM user_bloom_filter WHERE user_id = #{userId} AND is_deleted = 0 LIMIT 1")
    UserBloomFilter findByUserId(@Param("userId") Long userId);

    /**
     * 判断指定用户是否已存在布隆过滤器记录（排除已软删记录）。
     *
     * @param userId 用户 id
     * @return true 存在；false 不存在
     */
    @Select("SELECT EXISTS(SELECT 1 FROM user_bloom_filter WHERE user_id = #{userId} AND is_deleted = 0)")
    Boolean existsByUserId(@Param("userId") Long userId);
}