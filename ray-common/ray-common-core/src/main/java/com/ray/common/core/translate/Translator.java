package com.ray.common.core.translate;

import java.util.Map;
import java.util.Set;

/**
 * 翻译器 SPI：将一批原始 key 批量转换为展示文本。
 * <p>
 * 同一 type 在 Spring 容器中应仅有一个实现。所有实现自动被
 * {@code TranslateRegistry} 收集并按 {@link #type()} 分派。
 * <p>
 * 实现者需保证单次 {@link #batchTranslate(Set, TranslateMeta)} 内不出现 N+1
 * （如一次 IN 查询、或复用已有的 Spring Cache 字典服务）。
 */
public interface Translator {

    /**
     * 类型标识。对应 {@link Translate#type()} 的值。内置：{@code "dict"}、{@code "entity"}。
     */
    String type();

    /**
     * 批量翻译。
     *
     * @param keys 去重后的原始 key 集合
     * @param meta 本批翻译的元数据（确保 {@link TranslateKey#metaKeyOf(TranslateMeta)} 相同）
     * @return key → text 映射；无对应值时 key 可缺失或 value 为 null
     */
    Map<Object, String> batchTranslate(Set<Object> keys, TranslateMeta meta);
}
