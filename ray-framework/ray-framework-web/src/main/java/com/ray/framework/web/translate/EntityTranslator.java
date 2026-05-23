package com.ray.framework.web.translate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ray.common.core.translate.TranslateMeta;
import com.ray.common.core.translate.Translator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * type="entity" 的通用翻译器：按 {@link TranslateMeta#entity()} 类定位对应 {@link BaseMapper}，
 * 一次 {@code selectMaps(IN)} 查询批量翻译。
 * <p>
 * Mapper 解析：通过 Spring 泛型类型查找 {@code BaseMapper<T>} Bean；结果缓存到 ConcurrentHashMap。
 * 列名解析：通过 MyBatis-Plus {@link TableInfo} 将 Java 字段名转为数据库列名；
 * keyField 为 "id" 时优先使用主键列。
 */
@Component
public class EntityTranslator implements Translator {

    private final ApplicationContext applicationContext;
    private final Map<Class<?>, BaseMapper<?>> mapperCache = new ConcurrentHashMap<>();

    public EntityTranslator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public String type() {
        return "entity";
    }

    @Override
    public Map<Object, String> batchTranslate(Set<Object> keys, TranslateMeta meta) {
        Class<?> entityClass = meta.entity();
        if (entityClass == null || entityClass == Void.class) {
            throw new IllegalStateException("@Translate(type=\"entity\") requires entity class");
        }
        if (!StringUtils.hasText(meta.valueField())) {
            throw new IllegalStateException(
                    "@Translate(type=\"entity\") requires valueField for " + entityClass.getName());
        }

        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo == null) {
            throw new IllegalStateException(
                    "No MyBatis-Plus TableInfo for " + entityClass.getName() + "; is it mapped?");
        }
        String keyColumn = resolveKeyColumn(tableInfo, meta.keyField());
        String valueColumn = resolveValueColumn(tableInfo, meta.valueField());

        BaseMapper<?> mapper = resolveMapper(entityClass);
        QueryWrapper<Object> wrapper = Wrappers.query();
        wrapper.select(keyColumn, valueColumn);
        wrapper.in(keyColumn, keys);

        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Map<String, Object>> rows = ((BaseMapper) mapper).selectMaps(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }

        Map<Object, String> result = new HashMap<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            Object k = row.get(keyColumn);
            Object v = row.get(valueColumn);
            if (k == null || v == null) {
                continue;
            }
            result.put(k, String.valueOf(v));
        }
        return result;
    }

    private String resolveKeyColumn(TableInfo tableInfo, String keyField) {
        if (!StringUtils.hasText(keyField) || "id".equals(keyField)) {
            return tableInfo.getKeyColumn();
        }
        return resolveColumnByProperty(tableInfo, keyField);
    }

    private String resolveValueColumn(TableInfo tableInfo, String valueField) {
        return resolveColumnByProperty(tableInfo, valueField);
    }

    private String resolveColumnByProperty(TableInfo tableInfo, String property) {
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            if (fieldInfo.getProperty().equals(property)) {
                return fieldInfo.getColumn();
            }
        }
        if (property.equals(tableInfo.getKeyProperty())) {
            return tableInfo.getKeyColumn();
        }
        throw new IllegalStateException(
                "Property '" + property + "' not found in TableInfo of "
                        + tableInfo.getEntityType().getName());
    }

    private BaseMapper<?> resolveMapper(Class<?> entityClass) {
        BaseMapper<?> cached = mapperCache.get(entityClass);
        if (cached != null) {
            return cached;
        }
        // 优先：Spring 按泛型类型解析
        ResolvableType mapperType = ResolvableType.forClassWithGenerics(BaseMapper.class, entityClass);
        ObjectProvider<?> provider = applicationContext.getBeanProvider(mapperType);
        Object mapper = provider.getIfAvailable();
        if (mapper == null) {
            // 兜底：遍历所有 BaseMapper bean 按实际泛型参数匹配（MyBatis 代理场景）
            mapper = findMapperByIntrospection(entityClass);
        }
        if (mapper == null) {
            throw new IllegalStateException(
                    "No BaseMapper<" + entityClass.getName() + "> bean found in Spring context");
        }
        BaseMapper<?> baseMapper = (BaseMapper<?>) mapper;
        mapperCache.putIfAbsent(entityClass, baseMapper);
        return baseMapper;
    }

    private BaseMapper<?> findMapperByIntrospection(Class<?> entityClass) {
        Map<String, BaseMapper> beans = applicationContext.getBeansOfType(BaseMapper.class);
        for (BaseMapper<?> bean : beans.values()) {
            for (Class<?> iface : bean.getClass().getInterfaces()) {
                if (!BaseMapper.class.isAssignableFrom(iface)) {
                    continue;
                }
                ResolvableType resolved = ResolvableType.forClass(iface).as(BaseMapper.class);
                Class<?> arg = resolved.resolveGeneric(0);
                if (entityClass.equals(arg)) {
                    return bean;
                }
            }
        }
        return null;
    }
}
