package com.ray.common.core.translate;

/**
 * 翻译结果缓存的定位键：同一请求内 (type + meta 指纹 + 原始 key) 唯一定位一条翻译文本。
 *
 * @param type        Translator 标识
 * @param metaKey     同类型下的 meta 指纹（字典场景为 dictType；实体场景为 "entity#keyField#valueField"）
 * @param originalKey 源字段的原始值（ID / code 等）
 */
public record TranslateKey(String type, String metaKey, Object originalKey) {

    public TranslateKey {
        originalKey = normalizeKey(originalKey);
    }

    /**
     * 构造该条翻译记录对应的缓存键。
     */
    public static TranslateKey of(TranslateMeta meta, Object originalKey) {
        return new TranslateKey(meta.type(), metaKeyOf(meta), originalKey);
    }

    /**
     * 同一 type 下区分不同翻译目标（字典类型 / 实体 + 字段）的指纹。
     */
    public static String metaKeyOf(TranslateMeta meta) {
        return switch (meta.type()) {
            case "dict" -> meta.value();
            case "entity" -> meta.entity().getName() + "#" + meta.keyField() + "#" + meta.valueField();
            default -> meta.value() + "#" + meta.keyField() + "#" + meta.valueField();
        };
    }

    /**
     * 统一缓存 key 表达，避免源字段 Integer/String 与数据库返回 Long 等类型不一致时无法命中。
     */
    private static Object normalizeKey(Object key) {
        return switch (key) {
            case null -> null;
            case CharSequence s -> s.toString();
            case Number n -> n.toString();
            case Boolean b -> b.toString();
            case Enum<?> e -> e.name();
            default -> key;
        };
    }
}
