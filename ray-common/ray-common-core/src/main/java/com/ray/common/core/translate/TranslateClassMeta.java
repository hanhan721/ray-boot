package com.ray.common.core.translate;

import java.util.List;

/**
 * 按 Class 一次性解析出的翻译结构：
 * <ul>
 *   <li>{@code translateFields}：带 {@code @Translate} 注解的字段；</li>
 *   <li>{@code nestedGetters}：可能嵌套翻译目标的对象 / 容器字段，
 *       collector 递归扫描时深入其值。</li>
 * </ul>
 * 纯数据；通过 {@link TranslateMetaResolver#resolve(Class)} 获取（ClassValue 缓存）。
 */
public record TranslateClassMeta(
        List<TranslateField> translateFields,
        List<NestedGetter> nestedGetters
) {

    public static final TranslateClassMeta EMPTY =
            new TranslateClassMeta(List.of(), List.of());

    public boolean isEmpty() {
        return translateFields.isEmpty() && nestedGetters.isEmpty();
    }
}
