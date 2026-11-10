package com.ray.schedule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 调度停机配置；中断后仍未退出的任务会继续等待，容器宽限期须覆盖实际收尾时间。 */
@ConfigurationProperties(prefix = "ray.schedule")
public record ScheduleProperties(Integer shutdownGraceSeconds, Integer shutdownInterruptSeconds) {
    /** 补齐缺失的运行默认值，非法停机等待在启动绑定阶段即失败。 */
    public ScheduleProperties {
        // 1.缺失配置使用 30 秒优雅等待，中断后每等待 30 秒仍未退出则告警。
        if (shutdownGraceSeconds == null) shutdownGraceSeconds = 30;
        if (shutdownInterruptSeconds == null) shutdownInterruptSeconds = 30;
        // 2.限制等待与告警间隔，显式错误配置不能静默回退。
        if (shutdownGraceSeconds < 1 || shutdownGraceSeconds > 600
                || shutdownInterruptSeconds < 1 || shutdownInterruptSeconds > 600) {
            throw new IllegalArgumentException("非法调度停机等待秒数配置");
        }
    }
}
