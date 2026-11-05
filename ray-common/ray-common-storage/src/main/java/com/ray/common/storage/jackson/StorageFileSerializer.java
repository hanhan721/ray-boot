package com.ray.common.storage.jackson;

import com.ray.common.storage.annotation.AccessLevel;
import com.ray.common.storage.annotation.StorageFile;
import com.ray.common.storage.service.FileResponseResolver.FileView;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import java.util.List;
import java.util.Map;

/** 仅消费响应预扫描结果，序列化时不发起查询或签名。 */
public class StorageFileSerializer extends ValueSerializer<Object> {
    public static final String CACHE_ATTRIBUTE = "com.ray.storage.files";
    private final AccessLevel level;
    public StorageFileSerializer() { this(AccessLevel.PRIVATE); }
    private StorageFileSerializer(AccessLevel level) { this.level = level; }

    /** 读取字段声明的寿命档位，创建字段级序列化器，避免不同字段共享可变状态。 */
    @Override
    public ValueSerializer<?> createContextual(SerializationContext context, BeanProperty property) {
        var annotation = property == null ? null : property.getAnnotation(StorageFile.class);
        return annotation == null ? this : new StorageFileSerializer(annotation.level());
    }

    /** 按原有单值或列表结构输出文件信息，列表顺序与输入文件 ID 顺序一致。 */
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext context) {
        if (value instanceof List<?> ids) {
            gen.writeStartArray();
            for (var id : ids) this.write(id, gen, context);
            gen.writeEndArray();
        } else {
            this.write(value, gen, context);
        }
    }

    /** 从当前响应预加载缓存读取单个文件；未解析到的 ID 输出 null，不降级查询或泄露 key。 */
    private void write(Object value, JsonGenerator gen, SerializationContext context) {
        // 1.只接受 Long 文件 ID，拒绝旧路径字符串及其他不符合注解契约的类型。
        if (!(value instanceof Long id)) {
            if (value == null) { gen.writeNull(); return; }
            throw new IllegalArgumentException("@StorageFile 仅支持 Long / List<Long>");
        }
        // 2.按字段寿命档位和文件 ID 读取本次响应缓存，权限过滤已在预扫描阶段完成。
        var attribute = context.getAttribute(CACHE_ATTRIBUTE);
        var levels = attribute instanceof Map<?, ?> map ? map : Map.of();
        var entries = levels.get(level) instanceof Map<?, ?> map ? map : Map.of();
        if (!(entries.get(id) instanceof FileView(Long fileId, String name, String url, String ext, long size))) { gen.writeNull(); return; }
        // 3.只输出对外约定字段，不输出 hash、bucket 或对象 key。
        gen.writeStartObject();
        gen.writeNumberProperty("fileId", fileId);
        gen.writeStringProperty("name", name);
        gen.writeStringProperty("url", url);
        gen.writeStringProperty("ext", ext);
        gen.writeNumberProperty("size", size);
        gen.writeEndObject();
    }
}
