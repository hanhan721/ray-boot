package com.ray.common.security.service;

import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.constant.AuthCacheConstants;
import com.ray.common.security.domain.LoginUser;
import com.ray.common.valkey.util.ValkeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

        valkeyUtils.hSetAll(tokenKey, fields);
        valkeyUtils.expire(tokenKey, accessExpire, TimeUnit.MINUTES);

        // 添加到用户 Token 索引 Set
        var userTokensKey = AuthCacheConstants.USER_TOKENS_KEY_PREFIX + loginUser.userId();
        valkeyUtils.sAdd(userTokensKey, uuid);
        valkeyUtils.expire(userTokensKey, authProperties.getToken().getRefreshExpire(), TimeUnit.DAYS);

        log.debug("创建 Access Token: user={}, token={}", loginUser.username(), uuid);
        return uuid;
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
        valkeyUtils.sRemove(AuthCacheConstants.USER_TOKENS_KEY_PREFIX + userId, uuid);
        log.debug("删除 Access Token: userId={}, token={}", userId, uuid);
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
                    .map(t -> AuthCacheConstants.TOKEN_KEY_PREFIX + t)
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
