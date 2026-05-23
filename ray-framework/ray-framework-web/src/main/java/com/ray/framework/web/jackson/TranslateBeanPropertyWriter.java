package com.ray.framework.web.jackson;

import com.ray.common.core.translate.TranslateField;
import com.ray.common.core.translate.TranslateKey;
import com.ray.common.core.translate.TranslateMeta;
import com.ray.common.security.evaluator.PermissionEvaluator;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;

import java.util.Map;

/**
 * 为带 {@link com.ray.common.core.translate.Translate} 注解的字段追加一个虚拟字段，
 * 序列化时从 {@link SerializationContext} 的 attribute 中读取预先批量查好的翻译文本并输出。
 * <p>
 * 通过继承源字段原始 {@link BeanPropertyWriter} 复用其 member/annotations/declaredType，
 * 但使用独立的目标字段名与 String 直接写入，绕过源字段的类型序列化器。
 */
public class TranslateBeanPropertyWriter extends BeanPropertyWriter {

    /**
     * 传递翻译缓存所用的 SerializationContext attribute key。
     */
    public static final String TRANSLATE_CACHE_ATTR = "com.ray.translate.cache";

    private final TranslateField translateField;
    private final String targetName;
    private final PermissionEvaluator permissionEvaluator;
    private final String permission;

    public TranslateBeanPropertyWriter(BeanPropertyWriter base, TranslateField translateField,
                                       PermissionEvaluator permissionEvaluator, String permission) {
        super(base, PropertyName.construct(translateField.meta().target()));
        this.translateField = translateField;
        this.targetName = translateField.meta().target();
        this.permissionEvaluator = permissionEvaluator;
        this.permission = permission;
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator g, SerializationContext ctxt)
            throws Exception {
        if (!this.canWrite()) {
            return;
        }
        String text = lookup(bean, ctxt);
        if (text == null) {
            return;
        }
        g.writeName(_name);
        g.writeString(text);
    }

    @Override
    public void serializeAsElement(Object bean, JsonGenerator g, SerializationContext ctxt)
            throws Exception {
        if (!this.canWrite()) {
            super.serializeAsOmittedElement(bean, g, ctxt);
            return;
        }
        String text = lookup(bean, ctxt);
        if (text == null) {
            super.serializeAsOmittedElement(bean, g, ctxt);
            return;
        }
        g.writeString(text);
    }

    private String lookup(Object bean, SerializationContext ctxt) {
        Object cacheObj = ctxt.getAttribute(TRANSLATE_CACHE_ATTR);
        if (!(cacheObj instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return null;
        }
        Object key = translateField.readKey(bean);
        if (key == null) {
            return null;
        }
        TranslateMeta meta = translateField.meta();
        Object val = rawMap.get(new TranslateKey(meta.type(), TranslateKey.metaKeyOf(meta), key));
        return val instanceof String s ? s : null;
    }

    public String getTargetName() {
        return targetName;
    }

    private boolean canWrite() {
        return this.permission == null || this.permissionEvaluator.has(this.permission);
    }
}
