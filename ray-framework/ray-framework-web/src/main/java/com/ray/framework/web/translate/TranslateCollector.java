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
import com.ray.common.storage.annotation.AccessLevel;
import com.ray.common.storage.annotation.StorageFile;
import com.ray.common.storage.service.FileResponseResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 复用响应对象扫描，同时收集翻译 key 与 {@code @StorageFile} 文件 ID。
 * 先按字段权限过滤，再分组批量解析，产出仅用于当前响应序列化的缓存。
 */
@Component
public class TranslateCollector {

    private final TranslateRegistry registry;
    private final PermissionEvaluator permissionEvaluator;
    private final ObjectProvider<FileResponseResolver> fileResolver;

    public TranslateCollector(TranslateRegistry registry, PermissionEvaluator permissionEvaluator) {
        this(registry, permissionEvaluator, null);
    }

    @Autowired
    public TranslateCollector(TranslateRegistry registry, PermissionEvaluator permissionEvaluator,
                              ObjectProvider<FileResponseResolver> fileResolver) {
        this.registry = registry;
        this.permissionEvaluator = permissionEvaluator;
        this.fileResolver = fileResolver;
    }

    /**
     * @return 空 Map 表示无需翻译或无可翻译字段；否则为查询好的翻译缓存。
     */
    public Map<TranslateKey, String> collect(Object root) {
        return this.collectResponse(root, true).translations();
    }

    /**
     * 单次遍历响应对象图，按翻译元数据和文件寿命档位去重后批量查询。
     * translate 只控制字段翻译，文件解析仍执行；禁止进入无权限字段及其嵌套对象。
     */
    public ResponseCache collectResponse(Object root, boolean translate) {
        // 1.初始化本次响应的分组、权限结果和对象身份集合，避免循环引用重复扫描。
        if (root == null) {
            return new ResponseCache(Map.of(), Map.of());
        }
        Map<String, Group> groups = new HashMap<>();
        Map<AccessLevel, Set<Long>> fileGroups = new HashMap<>();
        Map<String, Boolean> permissionCache = new HashMap<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(root);

        // 2.以显式栈遍历响应对象图，统一收集可见翻译字段和文件 ID。
        while (!stack.isEmpty()) {
            Object current = stack.pop();
            if (current == null || !visited.add(current)) {
                continue;
            }
            this.traverse(current, groups, stack, permissionCache, fileGroups, translate);
        }

        // 3.按寿命档位批量解析去重文件 ID，序列化阶段不再逐字段查库或签名。
        Map<AccessLevel, Map<Long, FileResponseResolver.FileView>> fileCache = new HashMap<>();
        if (!fileGroups.isEmpty()) {
            var resolver = fileResolver == null ? null : fileResolver.getIfAvailable();
            if (resolver == null) throw new IllegalStateException("缺少文件解析服务");
            fileGroups.forEach((level, ids) -> fileCache.put(level, resolver.resolve(ids, level)));
        }

        // 4.按翻译类型与元数据批量查询文本，与文件解析结果一起返回。
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
        return new ResponseCache(cache, fileCache);
    }

    /** 展开响应容器，再扫描业务对象的可见字段；子对象交由外层栈统一遍历和去重。 */
    private void traverse(Object current, Map<String, Group> groups, Deque<Object> stack,
                          Map<String, Boolean> permissionCache, Map<AccessLevel, Set<Long>> fileGroups,
                          boolean translate) {
        // 1.展开统一响应、分页、集合、数组及 Map，避免将容器内部实现字段当作业务字段。
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

        // 2.先检查字段权限，再按寿命档位收集 Long / List<Long> 文件 ID。
        for (Field field : FileFields.CACHE.get(current.getClass())) {
            if (!this.canCollect(current.getClass(), field.getName(), permissionCache)) continue;
            try {
                var value = field.get(current);
                if (value == null) continue;
                var ids = fileGroups.computeIfAbsent(field.getAnnotation(StorageFile.class).level(), _ -> new HashSet<>());
                if (value instanceof Long id) ids.add(id);
                else if (value instanceof List<?> list) {
                    for (var item : list) {
                        if (item instanceof Long id) ids.add(id);
                        else if (item != null) throw new IllegalArgumentException("@StorageFile 需要 List<Long>");
                    }
                } else throw new IllegalArgumentException("@StorageFile 需要 Long / List<Long>");
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        // 3.仅在接口启用翻译时收集翻译 key，仍复用同一份字段权限结果。
        TranslateClassMeta classMeta = TranslateMetaResolver.resolve(current.getClass());
        if (classMeta.isEmpty()) {
            return;
        }
        for (TranslateField field : classMeta.translateFields()) {
            if (!translate) continue;
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
        // 4.通过权限检查的嵌套字段才入栈，阻止受保护对象内触发文件或翻译查询。
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

    /** 当前响应的查询结果快照，经 ObjectWriter attribute 传给字段序列化器。 */
    public record ResponseCache(Map<TranslateKey, String> translations,
            Map<AccessLevel, Map<Long, FileResponseResolver.FileView>> files) { }

    /** 按类缓存文件注解字段，只保存结构元数据，不保存用户权限或响应数据。 */
    private static final class FileFields {
        static final ClassValue<List<Field>> CACHE = new ClassValue<>() {
            @Override
            protected List<Field> computeValue(Class<?> type) {
                if (type.getName().startsWith("java.") || type.getName().startsWith("org.springframework.")
                        || type.getName().startsWith("com.baomidou.")) return List.of();
                var fields = new java.util.ArrayList<Field>();
                for (var cls = type; cls != null && cls != Object.class; cls = cls.getSuperclass()) {
                    for (var field : cls.getDeclaredFields()) {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                                || field.getAnnotation(StorageFile.class) == null) continue;
                        field.setAccessible(true);
                        fields.add(field);
                    }
                }
                return List.copyOf(fields);
            }
        };
    }

    /** 共享字段权限判断结果，保证预扫描过滤与最终 JSON 字段权限控制保持一致。 */
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
