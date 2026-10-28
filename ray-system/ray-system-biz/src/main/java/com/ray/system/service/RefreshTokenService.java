package com.ray.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.entity.RefreshToken;

/**
 * Refresh Token Service
 */
public interface RefreshTokenService extends IService<RefreshToken> {

    /**
     * 创建 Refresh Token
     *
     * @param rawToken    原始 Token（UUID）
     * @param userId      用户 ID
     * @param accessToken 关联的 Access Token UUID
     * @param userAgent   设备信息
     * @param ip          登录 IP
     */
    void create(String rawToken, Long userId, String accessToken, String userAgent, String ip);

    /**
     * 验证 Refresh Token
     *
     * @param rawToken 原始 Token（UUID）
     * @return 验证通过返回实体，否则返回 null
     */
    RefreshToken verify(String rawToken);

    /**
     * 根据 Token 哈希值删除
     *
     * @param tokenHash SHA-256 哈希值
     */
    void deleteByTokenHash(String tokenHash);

    /**
     * 删除用户所有 Refresh Token
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 删除指定 Access Token 关联的 Refresh Token（登出时清理会话残留）
     *
     * @param accessToken Access Token UUID
     */
    void deleteByAccessToken(String accessToken);

    /**
     * 清理过期的 Refresh Token 记录
     *
     * @return 清理数量
     */
    int cleanExpired();
}
