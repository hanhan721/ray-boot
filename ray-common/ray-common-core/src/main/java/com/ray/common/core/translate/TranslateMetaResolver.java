package com.ray.common.core.translate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 按 Class 解析并缓存翻译字段元数据。
 * <p>
 * 使用 {@link ClassValue} 实现：
 * <ul>
 *   <li>首次访问懒加载，无锁并发安全；</li>
 *   <li>类被卸载时自动释放缓存（无 ClassLoader 泄漏）；</li>
 *   <li>读取字段值使用 {@link MethodHandle}，JIT 友好。</li>
 * </ul>
 * <p>
 * 同时记录"嵌套候选字段"用于递归扫描，避免响应对象树里嵌套的单个对象被遗漏。
 */
public final class TranslateMetaResolver {

    private static final ClassValue<TranslateClassMeta> CACHE = new ClassValue<>() {
        @Override
        protected TranslateClassMeta computeValue(Class<?> type) {
            return TranslateMetaResolver.scan(type);
        }
    };

    private TranslateMetaResolver() {
    }

    public static TranslateClassMeta resolve(Class<?> clazz) {
        return CACHE.get(clazz);
    }

    private static TranslateClassMeta scan(Class<?> type) {
        if (type == null || type == Object.class || type.isPrimitive() || type.isArray()
                || type.isEnum() || type.isInterface()) {
            return TranslateClassMeta.EMPTY;
        }
        String pkg = type.getName();
        if (pkg.startsWith("java.") || pkg.startsWith("jakarta.") || pkg.startsWith("javax.")
                || pkg.startsWith("org.springframework.") || pkg.startsWith("com.baomidou.")) {
            return TranslateClassMeta.EMPTY;
        }

        List<TranslateField> translateFields = new ArrayList<>();
        List<NestedGetter> nestedGetters = new ArrayList<>();
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                    continue;
                }
                MethodHandle getter = unreflect(lookup, field);
                Translate annotation = field.getAnnotation(Translate.class);
                if (annotation != null) {
                    TranslateMeta meta = TranslateMeta.from(field.getName(), annotation);
                    translateFields.add(new TranslateField(
                            field.getName(), field.getType(), getter, meta));
                    continue;
                }
                if (mayNest(field.getType())) {
                    nestedGetters.add(new NestedGetter(field.getName(), getter));
                }
            }
            current = current.getSuperclass();
        }

        if (translateFields.isEmpty() && nestedGetters.isEmpty()) {
            return TranslateClassMeta.EMPTY;
        }
        return new TranslateClassMeta(List.copyOf(translateFields), List.copyOf(nestedGetters));
    }

    private static MethodHandle unreflect(MethodHandles.Lookup lookup, Field field) {
        try {
            field.setAccessible(true);
            return lookup.unreflectGetter(field)
                    .asType(MethodType.methodType(Object.class, Object.class));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Failed to unreflect getter: " + field.getDeclaringClass().getName() + "#" + field.getName(), e);
        }
    }

    /**
     * 判断字段类型是否可能嵌套翻译目标（粗略判定，具体还看值的运行时类型）。
     */
    private static boolean mayNest(Class<?> t) {
        if (t.isPrimitive()) {
            return false;
        }
        if (t == String.class || Number.class.isAssignableFrom(t) || t == Boolean.class
                || t == Character.class || t.isEnum()) {
            return false;
        }
        if (t.getName().startsWith("java.time.") || t.getName().startsWith("java.math.")) {
            return false;
        }
        // 集合 / Map / 数组 / 用户自定义 POJO
        return Collection.class.isAssignableFrom(t)
                || Map.class.isAssignableFrom(t)
                || t.isArray()
                || !t.getName().startsWith("java.");
    }
}
