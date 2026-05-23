package com.ray.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级权限控制注解
 * <p>
 * 标记在 VO/Entity 的字段上，序列化时自动检查当前用户是否拥有指定权限。
 * 无权限时该字段从 JSON 响应中完全移除（不是 null，而是 key 不存在）。
 * <p>
 * 适用于任何字段类型（String、Integer、Long、引用类型等）。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;FieldPermission("sample:cad:view")
 * private String cadDrawing;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldPermission {

    /**
     * 权限编码，与 {@code @PreAuthorize("@perm.has('xxx')")} 中的编码一致
     */
    String value();
}
