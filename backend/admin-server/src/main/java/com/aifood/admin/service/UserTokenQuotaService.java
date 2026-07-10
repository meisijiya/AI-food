package com.aifood.admin.service;

import com.ai.food.common.mapper.UserTokenQuotaMapper;
import com.ai.food.common.model.UserTokenQuota;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * per-user token 限额管理服务。
 *
 * <p>管理员通过此服务覆盖指定用户的每日 token 限额。
 * 查询时 getEffectiveLimit 走 ai-food-app 的 {@code TokenQuotaService}，
 * 这里只做管理端 CRUD。</p>
 */
@Service
@RequiredArgsConstructor
public class UserTokenQuotaService {

    private final UserTokenQuotaMapper userTokenQuotaMapper;

    /**
     * 查指定用户的限额覆盖记录。
     *
     * @param userId 用户 ID
     * @return 限额记录，null 表示未覆盖
     */
    public UserTokenQuota getByUserId(Long userId) {
        return userTokenQuotaMapper.findByUserId(userId);
    }

    /**
     * 设置或更新用户限额。
     *
     * @param userId     用户 ID
     * @param dailyLimit 每日 token 限额
     */
    public void upsert(Long userId, int dailyLimit) {
        UserTokenQuota existing = userTokenQuotaMapper.findByUserId(userId);
        if (existing == null) {
            UserTokenQuota quota = new UserTokenQuota();
            quota.setUserId(userId);
            quota.setDailyTokenLimit(dailyLimit);
            userTokenQuotaMapper.insert(quota);
        } else {
            existing.setDailyTokenLimit(dailyLimit);
            userTokenQuotaMapper.updateById(existing);
        }
    }

    /**
     * 删除用户限额覆盖，恢复全局默认。
     *
     * @param userId 用户 ID
     */
    public void delete(Long userId) {
        UserTokenQuota existing = userTokenQuotaMapper.findByUserId(userId);
        if (existing != null) {
            userTokenQuotaMapper.deleteById(existing.getId());
        }
    }
}
