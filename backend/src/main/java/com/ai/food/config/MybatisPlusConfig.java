package com.ai.food.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类
 * <p>
 * 注册 MyBatis-Plus 拦截器链与时间戳自动填充处理器。
 * 拦截器链顺序敏感：分页 → 乐观锁 → 防全表更新与删除。
 * </p>
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器链。
     * <p>
     * 包含三个内置拦截器：
     * <ul>
     *   <li>{@link PaginationInnerInterceptor} 分页插件，自动处理 LIMIT 与 COUNT</li>
     *   <li>{@link OptimisticLockerInnerInterceptor} 乐观锁插件，拦截 updateById 自动加 version 条件</li>
     *   <li>{@link BlockAttackInnerInterceptor} 防全表更新与删除，缺 WHERE 时抛异常</li>
     * </ul>
     * </p>
     *
     * @return 配置好的拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 分页：MySQL 方言，最大单页 500 条
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 2. 乐观锁：实体需含 @Version 字段
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 3. 防全表更新与删除：拦截无 WHERE 的 update/delete 语句
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * 时间戳自动填充处理器。
     * <p>
     * 替代原 JPA 的 @PrePersist / @PreUpdate 行为：
     * 插入时自动填 createdAt、updatedAt，更新时自动填 updatedAt。
     * </p>
     *
     * @return MetaObjectHandler 实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new TimeFieldMetaObjectHandler();
    }

    /**
     * 时间戳字段填充实现。
     * <p>
     * 替代原 JPA @PrePersist / @PreUpdate 行为：
     * <ul>
     *   <li>通用：createdAt / updatedAt 插入时填当前时间</li>
     *   <li>通用：updatedAt 更新时填当前时间</li>
     *   <li>BloomSyncLog：syncedAt 插入时填当前时间（原 @PrePersist 行为）</li>
     *   <li>CollectedParam：collectedAt 插入时填当前时间（原 @PrePersist 行为）</li>
     * </ul>
     * </p>
     */
    static class TimeFieldMetaObjectHandler implements MetaObjectHandler {

        @Override
        public void insertFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            // 通用字段
            strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
            strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            // 实体特定字段（strictInsertFill 不会因字段不存在而抛异常）
            strictInsertFill(metaObject, "syncedAt", LocalDateTime.class, now);
            strictInsertFill(metaObject, "collectedAt", LocalDateTime.class, now);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            // strictUpdateFill：仅当字段为 null 时填充（保留可能的显式设置）
            strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        }
    }
}
