package com.ai.food.mapper;

import com.ai.food.model.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户表 MyBatis-Plus Mapper 接口。
 *
 * <p>由原 JPA {@code UserRepository} 翻译而来，提供用户基础 CRUD 及复杂查询能力。
 * 所有显式 {@code @Select} 注解 SQL 均手动附加 {@code AND is_deleted = 0} 软删过滤条件，
 * 原因：{@code @TableLogic} 仅对 BaseMapper 自动生成的 select/delete 生效，
 * 自定义注解 SQL 不会自动追加过滤条件。
 */
// ponytail: 派生方法翻译为 @Select 注解而非 LambdaQueryWrapper，便于 SQL 审计与索引观察
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {

    /**
     * 按用户名查询单个用户。
     *
     * @param username 用户名
     * @return 匹配的用户实体，未命中返回 null（业务层使用 {@code Optional.ofNullable} 包装）
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0 LIMIT 1")
    SysUser findByUsername(@Param("username") String username);

    /**
     * 按邮箱查询单个用户。
     *
     * @param email 邮箱地址
     * @return 匹配的用户实体，未命中返回 null
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND is_deleted = 0 LIMIT 1")
    SysUser findByEmail(@Param("email") String email);

    /**
     * 判断指定用户名是否存在（排除已软删记录）。
     *
     * @param username 用户名
     * @return true 存在；false 不存在
     */
    @Select("SELECT EXISTS(SELECT 1 FROM sys_user WHERE username = #{username} AND is_deleted = 0)")
    Boolean existsByUsername(@Param("username") String username);

    /**
     * 判断指定邮箱是否存在（排除已软删记录）。
     *
     * @param email 邮箱地址
     * @return true 存在；false 不存在
     */
    @Select("SELECT EXISTS(SELECT 1 FROM sys_user WHERE email = #{email} AND is_deleted = 0)")
    Boolean existsByEmail(@Param("email") String email);

    /**
     * 按昵称模糊搜索用户，排除指定 id，支持分页。
     *
     * <p>SQL 中不附加 {@code LIMIT}，由 MyBatis-Plus 的 {@code PaginationInnerInterceptor}
     * 根据 {@code IPage} 参数自动注入分页条件。
     *
     * @param page      分页对象（MyBatis-Plus {@code IPage}）
     * @param keyword   昵称模糊匹配关键字
     * @param excludeId 需要排除的用户 id（通常为当前登录用户）
     * @return 分页结果，{@code records} 为当前页数据
     */
    @Select("SELECT * FROM sys_user WHERE nickname LIKE CONCAT('%', #{keyword}, '%') AND id != #{excludeId} AND is_deleted = 0")
    IPage<SysUser> searchUsers(IPage<SysUser> page,
                               @Param("keyword") String keyword,
                               @Param("excludeId") Long excludeId);

    /**
     * 按 id 集合批量查询用户。
     *
     * <p>使用 MyBatis {@code <foreach>} 注解语法展开 IN 列表。
     *
     * @param ids 用户 id 集合
     * @return 命中的用户列表（仅包含未软删记录）
     */
    @Select("<script>"
            + "SELECT * FROM sys_user WHERE is_deleted = 0 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>"
            + "#{id}"
            + "</foreach>"
            + "</script>")
    List<SysUser> findByIdIn(@Param("ids") List<Long> ids);
}