package com.ray.framework.log.annotation;

import com.ray.framework.log.enums.OperateType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * <p>
 * 标注在 Controller 方法上，自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperateLog {

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 操作类型
     */
    OperateType type() default OperateType.OTHER;

    /**
     * 是否记录请求参数
     */
    boolean saveRequestData() default true;

    /**
     * 是否记录响应数据
     */
    boolean saveResponseData() default true;
}
