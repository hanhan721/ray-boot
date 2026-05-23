package com.ray.common.core.translate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用响应字段翻译。
 * <p>
 * 标记在 Controller 类上时，该 Controller 下所有接口生效；
 * 标记在 Controller 方法上时，仅当前接口生效。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableTranslate {
}
