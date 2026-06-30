package com.aifood.admin.service;

import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.SysUser;
import com.aifood.admin.common.AdminException;
import com.aifood.admin.dto.UserQueryReq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理后台用户管理服务。
 *
 * <p>支持分页查询、详情、修改角色、禁用/启用。所有写操作均通过 MyBatis-Plus 的
 * {@code updateById} 走乐观锁（{@code @Version}）保证并发安全。</p>
 *
 * <p>注意 {@link SysUser#getIsDeleted()} 字段标了 {@code @TableLogic}，
 * 显式在 wrapper 里 {@code .eq(isDeleted, status)} 会覆盖自动软删过滤，
 * 允许同时查询启用 / 禁用的用户。</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * 分页查询用户。
     *
     * @param req 查询条件:keyword 模糊 / role 精确 / status 启用或禁用
     * @return MyBatis-Plus 分页结果,默认按 createdAt 倒序
     */
    public Page<SysUser> page(UserQueryReq req) {
        Page<SysUser> page = new Page<>(req.getPage(), req.getSize());
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            w.and(q -> q.like(SysUser::getUsername, req.getKeyword())
                .or().like(SysUser::getEmail, req.getKeyword())
                .or().like(SysUser::getNickname, req.getKeyword()));
        }
        if (StringUtils.hasText(req.getRole())) w.eq(SysUser::getRole, req.getRole());
        if (req.getStatus() != null) w.eq(SysUser::getIsDeleted, req.getStatus());
        // ponytail: startDate 接受 LocalDateTime 字符串("2026-06-29T00:00:00")或 LocalDate("2026-06-29")格式
        if (StringUtils.hasText(req.getStartDate())) w.ge(SysUser::getCreatedAt, req.getStartDate());
        if (StringUtils.hasText(req.getEndDate())) w.le(SysUser::getCreatedAt, req.getEndDate());
        w.orderByDesc(SysUser::getCreatedAt);
        return userMapper.selectPage(page, w);
    }

    /**
     * 查询单个用户详情。注:被禁用的用户也允许查询,便于审计回溯。
     *
     * @param id 用户 id
     * @return 用户实体
     * @throws AdminException 404 当用户不存在
     */
    public SysUser getDetail(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new AdminException(404, "用户不存在");
        return u;
    }

    /**
     * 修改用户角色。role 已被 Controller 层 @Pattern 校验为 USER / ADMIN。
     * 同值时跳过写库,避免无效更新 + 乐观锁版本号浪费。
     */
    public void updateRole(Long id, String role) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new AdminException(404, "用户不存在");
        if (role.equals(u.getRole())) return;  // no-op
        u.setRole(role);
        userMapper.updateById(u);
    }

    /** 禁用用户:is_deleted=1。注意是软删,数据仍保留以便审计追溯。 */
    public void disable(Long id) {
        if (userMapper.selectById(id) == null) throw new AdminException(404, "用户不存在");
        // ponytail: 用 LambdaUpdateWrapper 显式 set is_deleted,
        // 走 updateById(entity) 时 @TableLogic 字段会被自动排除,无法生效。
        LambdaUpdateWrapper<SysUser> w = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getIsDeleted, 1);
        userMapper.update(null, w);
    }

    /** 启用用户:is_deleted=0。 */
    public void enable(Long id) {
        if (userMapper.selectById(id) == null) throw new AdminException(404, "用户不存在");
        LambdaUpdateWrapper<SysUser> w = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getIsDeleted, 0);
        userMapper.update(null, w);
    }
}
