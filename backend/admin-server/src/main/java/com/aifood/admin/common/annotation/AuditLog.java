package com.aifood.admin.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法需要写操作审计日志。
 *
 * <p>审计切面 {@link com.aifood.admin.common.audit.AuditAspect} 会自动写入
 * {@code admin_audit_log} 表。要正确记录 target_id,请通过
 * {@link #targetParamIndex()} 显式声明 target 在方法参数中的位置:</p>
 *
 * <pre>{@code
 * @AuditLog(value="...", action="...", targetParamIndex = 0)
 * public void foo(@PathVariable Long id, ...) { ... }
 * }</pre>
 *
 * <p>未声明时 ({@code targetParamIndex = -1}) 走 fallback:按参数名
 * "id" 找第一个 Long/Integer/String 参数。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    String value();            // 描述,如 "修改用户角色"
    String action();           // 动作码,如 "UPDATE_USER_ROLE"
    int targetParamIndex() default -1;  // target id 在方法参数中的索引,-1 表示按参数名 "id" 启发式查找
}
