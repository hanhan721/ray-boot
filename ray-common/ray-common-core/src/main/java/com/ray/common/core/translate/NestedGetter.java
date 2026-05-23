package com.ray.common.core.translate;

import java.lang.invoke.MethodHandle;

/**
 * 嵌套字段 getter 包装：collector 遇到带 {@code @Translate} 字段的 Bean 时，
 * 也会递归扫描这些字段的值，以覆盖"嵌套单对象"场景（如 OrderVO.customer）。
 */
public record NestedGetter(String fieldName, MethodHandle getter) {

    public Object read(Object bean) {
        try {
            return getter.invoke(bean);
        } catch (Throwable t) {
            throw new IllegalStateException(
                    "Failed to read nested field: " + fieldName, t);
        }
    }
}
