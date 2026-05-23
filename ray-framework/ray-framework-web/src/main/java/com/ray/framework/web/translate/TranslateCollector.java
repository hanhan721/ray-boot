package com.ray.framework.web.translate;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.core.domain.R;
import com.ray.common.core.translate.NestedGetter;
import com.ray.common.core.translate.TranslateClassMeta;
import com.ray.common.core.translate.TranslateField;
import com.ray.common.core.translate.TranslateKey;
import com.ray.common.core.translate.TranslateMeta;
import com.ray.common.core.translate.TranslateMetaResolver;
import com.ray.common.core.translate.Translator;
import com.ray.common.security.annotation.FieldPermission;
import com.ray.common.security.evaluator.PermissionEvaluator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 递归扫描响应对象，收集所有 {@code @Translate} 字段的原始 key，
 * 按 Translator 分组发起批量查询，产出 {@link TranslateKey} → 翻译文本的映射。
 */
@Component
public class TranslateCollector {

    private final TranslateRegistry registry;
    private final PermissionEvaluator permissionEvaluator;

    public TranslateCollector(TranslateRegistry registry, PermissionEvaluator permissionEvaluator) {
        this.registry = registry;
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * @return 空 Map 表示无需翻译或无可翻译字段；否则为查询好的翻译缓存。
     */
    public Map<TranslateKey, String> collect(Object root) {
        if (root == null) {
            return Map.of();
        }
        Map<String, Group> groups = new HashMap<>();
        Map<String, Boolean> permissionCache = new HashMap<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Object current = stack.pop();
            if (current == null || !visited.add(current)) {
                continue;
            }
            traverse(current, groups, stack, permissionCache);
        }

        if (groups.isEmpty()) {
            return Map.of();
        }

        Map<TranslateKey, String> cache = new HashMap<>();
        for (Group group : groups.values()) {
            if (group.keys.isEmpty()) {
                continue;
            }
            Translator translator = registry.require(group.meta.type());
            Map<Object, String> translated = translator.batchTranslate(group.keys, group.meta);
            if (CollectionUtils.isEmpty(translated)) {
                continue;
            }
            String metaKey = TranslateKey.metaKeyOf(group.meta);
            for (Map.Entry<Object, String> entry : translated.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                cache.put(new TranslateKey(group.meta.type(), metaKey, entry.getKey()), entry.getValue());
            }
        }
        return cache;
    }

    private void traverse(Object current, Map<String, Group> groups, Deque<Object> stack,
                          Map<String, Boolean> permissionCache) {
        if (current instanceof R<?> response) {
            Object data = response.getData();
            if (data != null) {
                stack.push(data);
            }
            return;
        }
        if (current instanceof IPage<?> page) {
            List<?> records = page.getRecords();
            if (!CollectionUtils.isEmpty(records)) {
                for (Object e : records) {
                    if (e != null) {
                        stack.push(e);
                    }
                }
            }
            return;
        }
        if (current instanceof Iterable<?> it) {
            for (Object e : it) {
                if (e != null) {
                    stack.push(e);
                }
            }
            return;
        }
        if (current instanceof Object[] arr) {
            for (Object e : arr) {
                if (e != null) {
                    stack.push(e);
                }
            }
            return;
        }
        if (current instanceof Map<?, ?> map) {
            for (Object v : map.values()) {
                if (v != null) {
                    stack.push(v);
                }
            }
            return;
        }

        TranslateClassMeta classMeta = TranslateMetaResolver.resolve(current.getClass());
        if (classMeta.isEmpty()) {
            return;
        }
        for (TranslateField field : classMeta.translateFields()) {
            if (!canCollect(current.getClass(), field.sourceFieldName(), permissionCache)) {
                continue;
            }
            Object key = field.readKey(current);
            if (key == null) {
                continue;
            }
            TranslateMeta meta = field.meta();
            String groupKey = meta.type() + "::" + TranslateKey.metaKeyOf(meta);
            groups.computeIfAbsent(groupKey, k -> new Group(meta)).keys.add(key);
        }
        // 深入嵌套字段，覆盖单嵌套对象 / 嵌套集合场景
        for (NestedGetter nested : classMeta.nestedGetters()) {
            if (!canCollect(current.getClass(), nested.fieldName(), permissionCache)) {
                continue;
            }
            Object value = nested.read(current);
            if (value != null) {
                stack.push(value);
            }
        }
    }

    private boolean canCollect(Class<?> beanClass, String sourceFieldName, Map<String, Boolean> permissionCache) {
        String permission = FieldPermissionCache.get(beanClass, sourceFieldName);
        if (permission == null) {
            return true;
        }
        return permissionCache.computeIfAbsent(permission, permissionEvaluator::has);
    }

    private static final class FieldPermissionCache {

        private static final ClassValue<Map<String, String>> CACHE = new ClassValue<>() {
            @Override
            protected Map<String, String> computeValue(Class<?> type) {
                Map<String, String> permissions = new HashMap<>();
                Class<?> current = type;
                while (current != null && current != Object.class) {
                    for (Field field : current.getDeclaredFields()) {
                        FieldPermission annotation = field.getAnnotation(FieldPermission.class);
                        if (annotation != null) {
                            permissions.putIfAbsent(field.getName(), annotation.value());
                        }
                    }
                    current = current.getSuperclass();
                }
                return Map.copyOf(permissions);
            }
        };

        private FieldPermissionCache() {
        }

        static String get(Class<?> beanClass, String fieldName) {
            return CACHE.get(beanClass).get(fieldName);
        }
    }

    private static final class Group {
        final TranslateMeta meta;
        final Set<Object> keys = new HashSet<>();

        Group(TranslateMeta meta) {
            this.meta = meta;
        }
    }
}
