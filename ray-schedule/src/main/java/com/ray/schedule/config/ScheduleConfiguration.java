package com.ray.schedule.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 注册调度模块配置属性；模块内其余组件均为注解扫描，无需在此声明 Bean。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ScheduleProperties.class)
public class ScheduleConfiguration {
}
