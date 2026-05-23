package com.ray.common.notify.spi;

import com.ray.common.notify.model.NotifyChannel;
import com.ray.common.notify.model.NotifyRequest;

/**
 * 通知发送器接口（每个渠道实现一个）
 */
public interface Notifier {

    /**
     * 是否支持该渠道
     */
    boolean supports(NotifyChannel channel);

    /**
     * 发送通知
     */
    void send(NotifyRequest request);
}
