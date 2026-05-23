package com.ray.common.storage.annotation;

import com.ray.common.storage.jackson.StorageFileDeserializer;
import com.ray.common.storage.jackson.StorageFileSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记存储 key 字段，序列化时自动输出结构化文件信息（path + url + name）
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = StorageFileSerializer.class)
@JsonDeserialize(using = StorageFileDeserializer.class)
public @interface StorageFile {

    AccessLevel level() default AccessLevel.PRIVATE;

    /**
     * 是否为多值字段（逗号分隔的多个存储 key）
     */
    boolean multiple() default false;

    /**
     * 是否解析原始文件名（需查询 sys_file 表，默认关闭以减少性能开销）
     */
    boolean resolveName() default false;
}
