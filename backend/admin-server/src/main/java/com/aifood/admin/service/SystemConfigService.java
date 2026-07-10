package com.aifood.admin.service;

import com.ai.food.common.mapper.SystemConfigMapper;
import com.ai.food.common.model.SystemConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 通用键值配置服务，封装 system_config 表的 CRUD。
 *
 * <p>与 ai-food-app 的 {@code TokenQuotaService} 共享同一张表，
 * 这里专供 admin 后台读取/写入，不处理聚合逻辑（如 getEffectiveLimit）。</p>
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    /**
     * 查配置值（不存在返回 null）。
     *
     * @param key 配置键
     * @return 配置值，或 null
     */
    public String getConfigValue(String key) {
        SystemConfig config = systemConfigMapper.findByKey(key);
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 查 int 配置值，解析异常或 null 时回退默认值。
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 解析后的 int 值
     */
    public int getIntConfigValue(String key, int defaultValue) {
        String value = getConfigValue(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 设置配置值（不存在则插入，存在则更新）。
     *
     * @param key         配置键
     * @param value       配置值
     * @param description 描述（仅插入时生效）
     */
    public void setConfigValue(String key, String value, String description) {
        SystemConfig existing = systemConfigMapper.findByKey(key);
        if (existing == null) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            systemConfigMapper.insert(config);
        } else {
            systemConfigMapper.updateByKey(key, value);
        }
    }
}
