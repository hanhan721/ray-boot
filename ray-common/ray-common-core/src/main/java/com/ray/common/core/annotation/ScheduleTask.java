package com.ray.common.core.annotation;

import java.lang.annotation.*;

/**
 * 标注允许被调度执行的任务 Bean
 * <p>
 * 只有标注了此注解的 Spring Bean 才能被定时任务调度器反射调用，
 * 未标注的 Bean 在创建/注册任务时会被拒绝。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScheduleTask {
}
