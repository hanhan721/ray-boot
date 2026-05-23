package com.ray.framework.web.jackson;

import com.ray.common.security.annotation.FieldPermission;
import com.ray.common.security.evaluator.PermissionEvaluator;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.List;

/**
 * Jackson 序列化修改器：实现 {@link FieldPermission} 注解的字段级权限控制。
 * <p>
 * 对标记了 {@code @FieldPermission} 的字段，在序列化时检查当前用户是否拥有对应权限，
 * 无权限时该字段从 JSON 输出中完全移除。
 */
public class FieldPermissionSerializerModifier extends ValueSerializerModifier {

    private final PermissionEvaluator permissionEvaluator;

    public FieldPermissionSerializerModifier(PermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                      BeanDescription.Supplier beanDesc,
                                                      List<BeanPropertyWriter> beanProperties) {
        for (int i = 0; i < beanProperties.size(); i++) {
            var writer = beanProperties.get(i);
            var annotation = writer.getAnnotation(FieldPermission.class);
            if (annotation != null) {
                beanProperties.set(i, new PermissionAwareBeanPropertyWriter(writer, annotation.value()));
            }
        }
        return beanProperties;
    }

    /**
     * 包装 BeanPropertyWriter，在序列化前检查权限
     */
    private class PermissionAwareBeanPropertyWriter extends BeanPropertyWriter {

        private final String permission;

        PermissionAwareBeanPropertyWriter(BeanPropertyWriter base, String permission) {
            super(base);
            this.permission = permission;
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator g, SerializationContext ctxt)
                throws Exception {
            if (permissionEvaluator.has(permission)) {
                super.serializeAsProperty(bean, g, ctxt);
            }
        }

        @Override
        public void serializeAsElement(Object bean, JsonGenerator g, SerializationContext ctxt)
                throws Exception {
            if (permissionEvaluator.has(permission)) {
                super.serializeAsElement(bean, g, ctxt);
            } else {
                super.serializeAsOmittedElement(bean, g, ctxt);
            }
        }
    }
}
