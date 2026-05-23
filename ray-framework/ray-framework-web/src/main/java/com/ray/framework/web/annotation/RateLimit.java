package com.ray.framework.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IP 限流注解
 * <p>
 * 标注在 Controller 方法上，基于客户端 IP 进行固定窗口计数限流
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 窗口内最大请求数
     */
    int max() default 5;

    /**
     * 窗口时间（秒）
     */
    int window() default 60;

    /**
     * 自定义限流 key（为空则使用请求 URI）
     */
    String key() default "";
}
