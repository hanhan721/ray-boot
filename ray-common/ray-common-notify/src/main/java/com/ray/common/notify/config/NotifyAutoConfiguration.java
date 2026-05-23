package com.ray.common.notify.config;

import com.ray.common.notify.spi.Notifier;
import com.ray.common.notify.spi.NotifyDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

/**
 * 通知模块自动配置
 */
@Slf4j
@AutoConfiguration
@ComponentScan("com.ray.common.notify")
public class NotifyAutoConfiguration {

    /**
     * 注册 NotifyDispatcher，自动收集所有 Notifier 实现并按 supports() 路由
     */
    @Bean
    public NotifyDispatcher notifyDispatcher(List<Notifier> notifiers) {
        return request -> {
            var channel = request.channel();
            var notifier = notifiers.stream()
                    .filter(n -> n.supports(channel))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "[Notify] 未找到支持渠道 [" + channel + "] 的 Notifier"));
            notifier.send(request);
        };
    }
}
