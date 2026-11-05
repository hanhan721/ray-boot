package com.ray.common.storage.annotation;

import com.ray.common.storage.jackson.StorageFileSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import tools.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.*;

/** Long / List<Long> 文件 ID。请求直接提交 ID，响应输出结构化文件信息。 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = StorageFileSerializer.class)
public @interface StorageFile {
    /** 只控制链接寿命，不授予访问权限。 */
    AccessLevel level() default AccessLevel.PRIVATE;
}
