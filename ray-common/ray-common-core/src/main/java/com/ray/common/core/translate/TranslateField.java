package com.ray.common.core.translate;

import java.lang.invoke.MethodHandle;

/**
 * 绑定到具体 Bean 字段的翻译条目：包含注解元数据、源字段名及取值的 {@link MethodHandle}。
 *
 * @param sourceFieldName 源字段名（例如 createBy）
 * @param sourceType      源字段类型（用于 key 归一化）
 * @param getter          源字段的 MethodHandle（unreflectGetter），签名 (Object)Object
 * @param meta            注解元数据
 */
public record TranslateField(
        String sourceFieldName,
        Class<?> sourceType,
        MethodHandle getter,
        TranslateMeta meta
) {

    /**
     * 读取 bean 上源字段的原始值。
     */
    public Object readKey(Object bean) {
        try {
            return getter.invoke(bean);
        } catch (Throwable t) {
            throw new IllegalStateException(
                    "Failed to read translate source field: " + sourceFieldName, t);
        }
    }
}
