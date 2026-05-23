package com.ray.framework.web.jackson;

import com.ray.common.core.translate.TranslateClassMeta;
import com.ray.common.core.translate.TranslateField;
import com.ray.common.core.translate.TranslateMetaResolver;
import com.ray.common.security.annotation.FieldPermission;
import com.ray.common.security.evaluator.PermissionEvaluator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对含有 {@code @Translate} 字段的类，在 BeanPropertyWriter 列表末尾追加虚拟翻译字段。
 * 翻译结果由 {@link com.ray.framework.web.translate.TranslateAwareJsonHttpMessageConverter}
 * 通过 ObjectWriter.withAttribute 提前注入到 SerializationContext。
 */
public class TranslateSerializerModifier extends ValueSerializerModifier {

    private final PermissionEvaluator permissionEvaluator;

    public TranslateSerializerModifier(PermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription.Supplier beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        Class<?> beanClass = beanDesc.getBeanClass();
        TranslateClassMeta meta = TranslateMetaResolver.resolve(beanClass);
        if (meta.translateFields().isEmpty()) {
            return beanProperties;
        }

        Map<String, BeanPropertyWriter> byJsonName = new HashMap<>(beanProperties.size() * 2);
        for (BeanPropertyWriter writer : beanProperties) {
            byJsonName.put(writer.getName(), writer);
        }

        List<BeanPropertyWriter> result = new ArrayList<>(beanProperties);
        for (TranslateField tf : meta.translateFields()) {
            String target = tf.meta().target();
            if (byJsonName.containsKey(target)) {
                continue;
            }
            BeanPropertyWriter sourceWriter = byJsonName.get(tf.sourceFieldName());
            if (sourceWriter == null) {
                // 字段可能被 @JsonIgnore 或其他策略剔除。
                continue;
            }
            FieldPermission fieldPermission = sourceWriter.getAnnotation(FieldPermission.class);
            String permission = fieldPermission == null ? null : fieldPermission.value();
            TranslateBeanPropertyWriter writer = new TranslateBeanPropertyWriter(
                    sourceWriter, tf, permissionEvaluator, permission);
            result.add(writer);
            byJsonName.put(target, writer);
        }
        return result;
    }
}
