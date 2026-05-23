package com.ray.system.api.domain;

/**
 * Token 续期响应
 *
 * @param accessToken 新的 Access Token UUID
 * @param expireIn    过期时间（秒）
 */
public record RefreshVO(
        String accessToken,
        long expireIn
) {
}
