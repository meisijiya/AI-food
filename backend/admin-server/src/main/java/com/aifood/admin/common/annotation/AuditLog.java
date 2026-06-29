package com.aifood.admin.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 标记方法需要写操作审计日志 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    String value();   // 描述,如 "修改用户角色"
    String action();  // 动作码,如 "UPDATE_USER_ROLE"
}
