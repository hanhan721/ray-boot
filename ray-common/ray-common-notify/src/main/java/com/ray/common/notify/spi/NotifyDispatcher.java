package com.ray.common.notify.spi;

import com.ray.common.notify.model.NotifyRequest;

/**
 * 通知分发器 — 统一入口，按渠道路由到对应 {@link Notifier}
 */
public interface NotifyDispatcher {

    /**
     * 分发通知请求
     *
     * @param request 通知请求
     * @throws IllegalArgumentException 若没有 Notifier 支持该渠道
     */
    void dispatch(NotifyRequest request);
}
