package com.ray.common.notify.model;

import java.util.Map;

/**
 * 通知请求（渠道无关）
 *
 * @param channel    通知渠道
 * @param target     接收目标（邮箱地址、手机号等）
 * @param templateId 模板标识
 * @param params     模板参数
 */
public record NotifyRequest(
        NotifyChannel channel,
        String target,
        String templateId,
        Map<String, Object> params
) {}
