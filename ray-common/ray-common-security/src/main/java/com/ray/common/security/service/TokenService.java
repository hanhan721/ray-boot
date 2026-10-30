package com.ray.common.security.service;

import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.constant.AuthCacheConstants;
import com.ray.common.security.domain.LoginUser;
import com.ray.common.valkey.util.ValkeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Token 核心服务
 * <p>
 * 管理 Access Token 在 Valkey 中的存储、验证、删除，
 * 以及用户权限缓存的读写操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final ValkeyUtils valkeyUtils;
    private final AuthProperties authProperties;

    /**
     * 创建 Access Token
     *
     * @param loginUser 登录用户信息
     * @param nickname  用户昵称
     * @param browser   浏览器
     * @param os        操作系统
     * @return Access Token UUID
     */
    public String createAccessToken(LoginUser loginUser, String nickname, String browser, String os) {
        var uuid = UUID.randomUUID().toString();
        var sessionId = this.sessionId(uuid);
        var accessExpire = authProperties.getToken().getAccessExpire();

        // 写入 Token 主体 Hash
        var tokenKey = AuthCacheConstants.TOKEN_KEY_PREFIX + uuid;
        Map<String, Object> fields = new HashMap<>();
        fields.put(AuthCacheConstants.FIELD_USER_ID, loginUser.userId());
        fields.put(AuthCacheConstants.FIELD_USERNAME, loginUser.username());
        fields.put(AuthCacheConstants.FIELD_NICKNAME, nickname != null ? nickname : "");
        fields.put(AuthCacheConstants.FIELD_ROLES, String.join(",", loginUser.roles()));
        fields.put(AuthCacheConstants.FIELD_LOGIN_TIME, loginUser.loginTime().toString());
        fields.put(AuthCacheConstants.FIELD_LOGIN_IP, loginUser.loginIp());
        fields.put(AuthCacheConstants.FIELD_BROWSER, browser != null ? browser : "Unknown");
        fields.put(AuthCacheConstants.FIELD_OS, os != null ? os : "Unknown");
        fields.put(AuthCacheConstants.FIELD_SESSION_ID, sessionId);

        valkeyUtils.hSetAll(tokenKey, fields);
        valkeyUtils.expire(tokenKey, accessExpire, TimeUnit.MINUTES);
        valkeyUtils.set(AuthCacheConstants.SESSION_KEY_PREFIX + sessionId, uuid,
                TimeUnit.MINUTES.toSeconds(accessExpire));

        // 添加到用户 Token 索引 Set
        var userTokensKey = AuthCacheConstants.USER_TOKENS_KEY_PREFIX + loginUser.userId();
        valkeyUtils.sAdd(userTokensKey, uuid);
        valkeyUtils.expire(userTokensKey, authProperties.getToken().getRefreshExpire(), TimeUnit.DAYS);

        log.debug("创建 Access Token: userId={}", loginUser.userId());
        return uuid;
    }

    /**
     * 为升级前签发的 Access Token 补充管理索引，过期时间不超过原令牌剩余寿命。
     */
    public String registerSession(String accessToken) {
        var remainingSeconds = valkeyUtils.getExpire(AuthCacheConstants.TOKEN_KEY_PREFIX + accessToken);
        if (remainingSeconds == null || remainingSeconds <= 0) {
            return null;
        }
        var sessionId = this.sessionId(accessToken);
        valkeyUtils.set(AuthCacheConstants.SESSION_KEY_PREFIX + sessionId, accessToken, remainingSeconds);
        return sessionId;
    }

    /**
     * 只允许管理标识查找会话，不接受原始 Access Token 作为管理标识。
     */
    public String getAccessTokenBySessionId(String sessionId) {
        if (sessionId == null || !sessionId.matches("[0-9a-f]{64}")) {
            return null;
        }
        return valkeyUtils.get(AuthCacheConstants.SESSION_KEY_PREFIX + sessionId, String.class);
    }

    private String sessionId(String accessToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(accessToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 验证 Access Token 并返回登录用户信息
     *
     * @param uuid Access Token UUID
     * @return 登录用户信息，Token 无效时返回 null
     */
    public LoginUser validateAccessToken(String uuid) {
        var tokenKey = AuthCacheConstants.TOKEN_KEY_PREFIX + uuid;
        var fields = valkeyUtils.hGetAll(tokenKey);

        if (CollectionUtils.isEmpty(fields)) {
            return null;
        }

        var userId = ((Number) fields.get(AuthCacheConstants.FIELD_USER_ID)).longValue();
        var username = (String) fields.get(AuthCacheConstants.FIELD_USERNAME);
        var rolesStr = (String) fields.get(AuthCacheConstants.FIELD_ROLES);
        var loginTimeStr = (String) fields.get(AuthCacheConstants.FIELD_LOGIN_TIME);
        var loginIp = (String) fields.get(AuthCacheConstants.FIELD_LOGIN_IP);

        Set<String> roles = (rolesStr == null || rolesStr.isBlank())
                ? Set.of()
                : Arrays.stream(rolesStr.split(",")).collect(Collectors.toSet());

        return new LoginUser(
                userId,
                username,
                roles,
                LocalDateTime.parse(loginTimeStr),
                loginIp
        );
    }

    /**
     * 删除单个 Access Token
     *
     * @param uuid   Access Token UUID
     * @param userId 用户 ID
     */
    public void removeAccessToken(String uuid, Long userId) {
        valkeyUtils.delete(AuthCacheConstants.TOKEN_KEY_PREFIX + uuid);
        valkeyUtils.delete(AuthCacheConstants.SESSION_KEY_PREFIX + this.sessionId(uuid));
        valkeyUtils.sRemove(AuthCacheConstants.USER_TOKENS_KEY_PREFIX + userId, uuid);
        log.debug("删除 Access Token: userId={}", userId);
    }

    /**
     * 删除用户的所有 Access Token（踢人下线）
     *
     * @param userId 用户 ID
     */
    public void removeAllTokens(Long userId) {
        var userTokensKey = AuthCacheConstants.USER_TOKENS_KEY_PREFIX + userId;
        var tokens = valkeyUtils.sMembers(userTokensKey);

        if (!CollectionUtils.isEmpty(tokens)) {
            var tokenKeys = tokens.stream()
                    .flatMap(t -> Stream.of(
                            AuthCacheConstants.TOKEN_KEY_PREFIX + t,
                            AuthCacheConstants.SESSION_KEY_PREFIX + this.sessionId(t.toString())))
                    .toList();
            valkeyUtils.delete(tokenKeys);
        }

        valkeyUtils.delete(userTokensKey);
        log.debug("删除用户所有 Access Token: userId={}, count={}", userId,
                tokens != null ? tokens.size() : 0);
    }

    /**
     * 缓存用户菜单（前端动态路由）
     *
     * @param userId 用户 ID
     * @param menuJson 菜单树 JSON 字符串
     */
    public void cacheMenus(Long userId, String menuJson) {
        var menuKey = AuthCacheConstants.MENU_KEY_PREFIX + userId;
        valkeyUtils.delete(menuKey);
        if (menuJson != null && !menuJson.isBlank()) {
            valkeyUtils.set(menuKey, menuJson);
        }
    }

    /**
     * 获取用户菜单缓存
     *
     * @param userId 用户 ID
     * @return 菜单树 JSON 字符串
     */
    public String getMenus(Long userId) {
        var menuKey = AuthCacheConstants.MENU_KEY_PREFIX + userId;
        return valkeyUtils.get(menuKey, String.class);
    }

    /**
     * 清除用户菜单缓存
     *
     * @param userId 用户 ID
     */
    public void removeMenus(Long userId) {
        valkeyUtils.delete(AuthCacheConstants.MENU_KEY_PREFIX + userId);
    }

    /**
     * 缓存用户权限
     *
     * @param userId      用户 ID
     * @param permissions 权限编码集合
     */
    public void cachePermissions(Long userId, Set<String> permissions) {
        var permKey = AuthCacheConstants.PERMISSION_KEY_PREFIX + userId;
        valkeyUtils.delete(permKey);
        if (!CollectionUtils.isEmpty(permissions)) {
            valkeyUtils.sAdd(permKey, permissions.toArray());
        }
    }

    /**
     * 获取用户权限缓存
     *
     * @param userId 用户 ID
     * @return 权限编码集合
     */
    public Set<String> getPermissions(Long userId) {
        var permKey = AuthCacheConstants.PERMISSION_KEY_PREFIX + userId;
        var members = valkeyUtils.sMembers(permKey);
        if (CollectionUtils.isEmpty(members)) {
            return Set.of();
        }
        return members.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    /**
     * 清除用户权限缓存
     *
     * @param userId 用户 ID
     */
    public void removePermissions(Long userId) {
        valkeyUtils.delete(AuthCacheConstants.PERMISSION_KEY_PREFIX + userId);
    }
}
