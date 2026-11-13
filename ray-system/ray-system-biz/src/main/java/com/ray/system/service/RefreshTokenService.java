package com.ray.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.entity.RefreshToken;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Refresh Token Service
 */
public interface RefreshTokenService extends IService<RefreshToken> {

    /**
     * 创建 Refresh Token
     *
     * @param rawToken    原始 Token（UUID）
     * @param userId      用户 ID
     * @param sessionId   关联的会话摘要
     * @param userAgent   设备信息
     * @param ip          登录 IP
     */
    void create(String rawToken, Long userId, String sessionId, String userAgent, String ip);

    /** 在同一次登录内签发轮换凭据。 */
    void create(String rawToken, Long userId, String sessionId, String loginSessionId, String userAgent, String ip);

    /** 查找尚未过期的凭据（包含已轮换记录），仅供登出定位。 */
    RefreshToken findForLogout(String rawToken);

    /** 保留旧凭据的撤销映射，禁止它再次续期。 */
    void markRotated(String tokenHash);

    /** 撤销指定登录及其轮换历史，返回需要撤销的当前 Access Token 摘要。 */
    Set<String> revokeLoginSession(Long userId, String loginSessionId);

    /**
     * 查询用户所有凭据关联的 Access Token 摘要。
     * <p>
     * 作为 Valkey 会话索引的持久化兜底，避免缓存淘汰导致批量撤销遗漏会话。
     *
     * @param userIds 用户 ID 集合
     * @return userId -> sessionId 集合
     */
    Map<Long, Set<String>> findSessionIdsByUserIds(Collection<Long> userIds);

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
     * 删除指定会话关联的 Refresh Token（登出时清理会话残留）
     *
     * @param sessionId 会话摘要
     */
    void deleteBySessionId(String sessionId);

    /**
     * 清理过期的 Refresh Token 记录
     *
     * @return 清理数量
     */
    int cleanExpired();
}
