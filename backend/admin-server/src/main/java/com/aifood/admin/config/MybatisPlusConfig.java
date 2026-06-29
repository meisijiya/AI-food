package com.aifood.admin.config;

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
 * admin-server MyBatis-Plus 配置。
 *
 * <p>与 ai-food-app 的同名配置对齐：
 * <ul>
 *   <li>{@link PaginationInnerInterceptor} 分页插件,自动处理 LIMIT 与 COUNT</li>
 *   <li>{@link OptimisticLockerInnerInterceptor} 乐观锁插件,SysUser.version 走 updateById 自动 +1</li>
 *   <li>{@link BlockAttackInnerInterceptor} 防全表 update/delete</li>
 *   <li>时间戳自动填充 {@link MetaObjectHandler}</li>
 * </ul>
 *
 * <p>admin-server 不依赖 ai-food-app,故需自带一份；拦截器与填充行为保持一致
 * 以避免两个模块对同一张表写入时行为分裂。</p>
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器链。
     * 顺序敏感：分页 → 乐观锁 → 防全表更新与删除。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /** createdAt / updatedAt 自动填充,与 ai-food-app 保持一致。 */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new TimeFieldMetaObjectHandler();
    }

    static class TimeFieldMetaObjectHandler implements MetaObjectHandler {

        @Override
        public void insertFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
            strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            strictInsertFill(metaObject, "syncedAt", LocalDateTime.class, now);
            strictInsertFill(metaObject, "collectedAt", LocalDateTime.class, now);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        }
    }
}
