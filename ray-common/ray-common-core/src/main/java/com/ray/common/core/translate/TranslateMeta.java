package com.ray.common.core.translate;

/**
 * 单个 {@link Translate} 注解解析后的元数据快照。
 *
 * @param type       Translator 标识
 * @param target     翻译后字段名（已解析，非空）
 * @param value      通用参数（如 dictType）
 * @param entity     实体类（type="entity" 场景），无则为 {@link Void}
 * @param keyField   实体主键字段名
 * @param valueField 实体展示字段名
 */
public record TranslateMeta(
        String type,
        String target,
        String value,
        Class<?> entity,
        String keyField,
        String valueField
) {

    public static TranslateMeta from(String sourceFieldName, Translate annotation) {
        String target = annotation.target();
        if (target == null || target.isBlank()) {
            target = sourceFieldName + "Text";
        }
        return new TranslateMeta(
                annotation.type(),
                target,
                annotation.value(),
                annotation.entity(),
                annotation.keyField(),
                annotation.valueField()
        );
    }
}
