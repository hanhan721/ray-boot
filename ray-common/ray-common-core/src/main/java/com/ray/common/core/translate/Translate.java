package com.ray.common.core.translate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段翻译注解：在序列化时为原字段追加一个翻译后的文本字段。
 * <p>
 * 示例：
 * <pre>{@code
 * // 字典翻译
 * @Translate(type = "dict", value = "order_status")
 * private Integer status;   // 输出额外字段 statusText: "已完成"
 *
 * // 实体翻译
 * @Translate(type = "entity", entity = SysUser.class, valueField = "nickname")
 * private Long createBy;    // 输出额外字段 createByText: "张三"
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Translate {

    /**
     * Translator 类型标识，对应 {@link Translator#type()} 返回值。
     * 内置：{@code "dict"}、{@code "entity"}。
     */
    String type();

    /**
     * 翻译后目标字段名。为空时使用默认约定：原字段名 + "Text"。
     */
    String target() default "";

    /**
     * 通用参数。type="dict" 时传字典类型（如 "order_status"）；
     * 其他自定义 Translator 按需约定。
     */
    String value() default "";

    /**
     * type="entity" 时必填：翻译源实体类。
     */
    Class<?> entity() default Void.class;

    /**
     * type="entity" 时的主键字段名，默认 "id"。
     */
    String keyField() default "id";

    /**
     * type="entity" 时的目标展示字段名（如 "nickname"、"deptName"）。
     */
    String valueField() default "";
}
