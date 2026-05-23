package com.ray.common.mybatis.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.baomidou.mybatisplus.core.toolkit.StringUtils.camelToUnderline;

/**
 * 根据 Query DTO 的字段和 {@link QueryColumn} 注解自动构建 {@link QueryWrapper}
 *
 * 规则：
 * - 只处理 Query DTO 自身声明的字段（不含父类，如 PageQuery 的分页字段）
 * - 无 @QueryColumn 注解的字段默认使用 EQ
 * - 标记 @QueryColumn(QueryType.IGNORE) 的字段被跳过
 * - 值为 null 或空白字符串的字段自动跳过
 * - 反射元数据按类缓存，只解析一次
 * - 实现 {@link Orderable} 接口的 Query DTO 自动应用排序（默认 id DESC）
 *
 * @see QueryColumn
 * @see QueryType
 * @see Orderable
 */
@Slf4j
public final class QueryWrapperHelper {

    private QueryWrapperHelper() {
    }

    /** 缓存：Query DTO class -> 字段元数据列表 */
    private static final Map<Class<?>, List<FieldMeta>> CACHE = new ConcurrentHashMap<>();

    /**
     * 根据 Query DTO 构建 QueryWrapper
     *
     * @param query Query DTO 实例
     * @return QueryWrapper（泛型由调用方推断）
     */
    public static <T> QueryWrapper<T> build(Object query) {
        var wrapper = new QueryWrapper<T>();
        if (query == null) {
            return wrapper;
        }
        var metas = CACHE.computeIfAbsent(query.getClass(), QueryWrapperHelper::parseFields);
        for (var meta : metas) {
            var value = meta.getValue(query);
            if (isEmpty(value)) {
                continue;
            }
            applyCondition(wrapper, meta.column, meta.queryType, value);
        }
        // 自动应用排序
        if (query instanceof Orderable orderable) {
            applyOrder(wrapper, orderable);
        }
        return wrapper;
    }

    // ===== 内部实现 =====

    /**
     * 解析 Query DTO 自身声明的字段（不含父类）
     */
    private static List<FieldMeta> parseFields(Class<?> clazz) {
        var metas = new ArrayList<FieldMeta>();
        for (var field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            var annotation = field.getAnnotation(QueryColumn.class);
            var type = annotation != null ? annotation.value() : QueryType.EQ;
            if (type == QueryType.IGNORE) {
                continue;
            }
            var column = (annotation != null && StringUtils.hasText(annotation.column()))
                    ? annotation.column()
                    : camelToUnderline(field.getName());
            field.setAccessible(true);
            metas.add(new FieldMeta(field, column, type));
        }
        return Collections.unmodifiableList(metas);
    }

    /**
     * 应用排序
     */
    private static void applyOrder(QueryWrapper<?> wrapper, Orderable orderable) {
        var column = orderable.orderColumn();
        if (StringUtils.hasText(column)) {
            if (orderable.orderAsc()) {
                wrapper.orderByAsc(column);
            } else {
                wrapper.orderByDesc(column);
            }
        }
    }

    /**
     * 应用单个查询条件
     */
    private static void applyCondition(QueryWrapper<?> wrapper, String column, QueryType type, Object value) {
        switch (type) {
            case EQ -> wrapper.eq(column, value);
            case NE -> wrapper.ne(column, value);
            case LIKE -> wrapper.like(column, value);
            case LEFT_LIKE -> wrapper.likeLeft(column, value);
            case RIGHT_LIKE -> wrapper.likeRight(column, value);
            case GT -> wrapper.gt(column, value);
            case GE -> wrapper.ge(column, value);
            case LT -> wrapper.lt(column, value);
            case LE -> wrapper.le(column, value);
            case IN -> applyIn(wrapper, column, value);
            default -> { }
        }
    }

    /**
     * 处理 IN 查询：支持逗号分隔字符串和 Collection
     */
    private static void applyIn(QueryWrapper<?> wrapper, String column, Object value) {
        switch (value) {
            case String s -> {
                var list = Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList();
                if (!list.isEmpty()) {
                    wrapper.in(column, list);
                }
            }
            case Collection<?> c -> {
                if (!c.isEmpty()) {
                    wrapper.in(column, c);
                }
            }
            default -> wrapper.in(column, value);
        }
    }

    /**
     * 判断值是否为空（null、空白字符串、空集合）
     */
    private static boolean isEmpty(Object value) {
        return switch (value) {
            case null -> true;
            case String s -> s.isBlank();
            case Collection<?> c -> c.isEmpty();
            default -> false;
        };
    }

    /**
     * 字段元数据（缓存后复用）
     */
    private record FieldMeta(Field field, String column, QueryType queryType) {
        Object getValue(Object obj) {
            try {
                return field.get(obj);
            } catch (IllegalAccessException e) {
                log.warn("读取查询字段失败: {}", field.getName(), e);
                return null;
            }
        }
    }
}
