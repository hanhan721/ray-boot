package com.ray.system.api.domain;

/**
 * 登录响应
 *
 * @param accessToken Access Token UUID
 * @param expireIn    过期时间（秒）
 */
public record LoginVO(
        String accessToken,
        long expireIn
) {
}
