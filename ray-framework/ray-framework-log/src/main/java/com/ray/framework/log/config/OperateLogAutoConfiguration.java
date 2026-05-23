package com.ray.framework.log.config;

import com.ray.framework.log.aspect.OperateLogAspect;
import com.ray.framework.log.handler.DefaultOperateLogHandler;
import com.ray.framework.log.handler.OperateLogHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * 操作日志自动配置
 */
@Configuration
public class OperateLogAutoConfiguration {

    /**
     * 默认日志处理器（可被业务模块覆盖）
     */
    @Bean
    @ConditionalOnMissingBean(OperateLogHandler.class)
    public OperateLogHandler operateLogHandler() {
        return new DefaultOperateLogHandler();
    }

    @Bean
    public OperateLogAspect operateLogAspect(OperateLogHandler operateLogHandler, JsonMapper jsonMapper) {
        return new OperateLogAspect(operateLogHandler, jsonMapper);
    }
}
