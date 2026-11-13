package com.ray.common.security.service;

import com.ray.common.core.constant.ResultCode;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.constant.AuthCacheConstants;
import com.ray.common.security.domain.LoginUser;
import com.ray.common.security.domain.TokenAuthentication;
import com.ray.common.security.domain.UserAuthorization;
import com.ray.common.valkey.util.ValkeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Token 核心服务
 * <p>
 * 管理 Access Token 在 Valkey 中的存储、验证、删除，
 * 以及当前用户授权快照的读取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final ValkeyUtils valkeyUtils;
    private final AuthProperties authProperties;
    private final UserAuthorizationProvider authorizationProvider;
    private final OnlineSessionIndex onlineSessionIndex;

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
        return this.createAccessToken(loginUser, nickname, browser, os, null);
    }

    /** 续期时沿用稳定的登录标识；Access Token 仍独立轮换。 */
    public String createAccessToken(LoginUser loginUser, String nickname, String browser, String os, String loginSessionId) {
        var authorization = authorizationProvider.getAuthorization(loginUser.userId());
        var uuid = UUID.randomUUID().toString();
        var sessionId = TokenService.hashAccessToken(uuid);
        var accessExpire = authProperties.getToken().getAccessExpire();

        // 写入会话 Hash
        var sessionKey = AuthCacheConstants.SESSION_KEY_PREFIX + sessionId;
        Map<String, Object> fields = new HashMap<>();
        fields.put(AuthCacheConstants.FIELD_USER_ID, loginUser.userId());
        fields.put(AuthCacheConstants.FIELD_LOGIN_SESSION_ID, loginSessionId != null ? loginSessionId : sessionId);
        fields.put(AuthCacheConstants.FIELD_USERNAME, loginUser.username());
        fields.put(AuthCacheConstants.FIELD_NICKNAME, nickname != null ? nickname : "");
        fields.put(AuthCacheConstants.FIELD_LOGIN_TIME, loginUser.loginTime().toString());
        fields.put(AuthCacheConstants.FIELD_LOGIN_IP, loginUser.loginIp());
        fields.put(AuthCacheConstants.FIELD_BROWSER, browser != null ? browser : "Unknown");
        fields.put(AuthCacheConstants.FIELD_OS, os != null ? os : "Unknown");
        // 使用生产序列化器支持的集合，读取时再转为不可变快照。
        fields.put(AuthCacheConstants.FIELD_ROLES, new LinkedHashSet<>(authorization.roles()));
        fields.put(AuthCacheConstants.FIELD_PERMISSIONS, new LinkedHashSet<>(authorization.permissions()));
        var created = Map.of(loginUser.userId(), Set.of(sessionId));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        TokenService.this.revokeCapturedSessions(created, false);
                    }
                }
            });
        }
        try {
            valkeyUtils.hSetAll(sessionKey, fields);
            valkeyUtils.expire(sessionKey, accessExpire, TimeUnit.MINUTES);
            onlineSessionIndex.add(sessionId, loginUser.loginTime());

            // 添加到用户会话索引 Set
            var userSessionsKey = AuthCacheConstants.USER_SESSIONS_KEY_PREFIX + loginUser.userId();
            valkeyUtils.sAdd(userSessionsKey, sessionId);
            valkeyUtils.expire(userSessionsKey, authProperties.getToken().getRefreshExpire(), TimeUnit.DAYS);
        } catch (RuntimeException failure) {
            this.revokeCapturedSessions(created, false);
            throw new BusinessException("会话签发失败，请重新登录");
        }
        log.debug("创建 Access Token: userId={}", loginUser.userId());
        return uuid;
    }

    public static String hashAccessToken(String accessToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(accessToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 对凭据求摘要后，一次 HGETALL 同时读取身份及授权快照。
     *
     * @param accessToken Access Token UUID
     * @return 完整认证对象，Token 无效时返回 null
     */
    public TokenAuthentication validateSession(String accessToken) {
        var sessionId = TokenService.hashAccessToken(accessToken);
        var sessionKey = AuthCacheConstants.SESSION_KEY_PREFIX + sessionId;
        var fields = valkeyUtils.hGetAll(sessionKey);
        if (fields == null || fields.isEmpty()) {
            return null;
        }

        if (!(fields.get(AuthCacheConstants.FIELD_USER_ID) instanceof Number userId)
                || !(fields.get(AuthCacheConstants.FIELD_USERNAME) instanceof String username)
                || !(fields.get(AuthCacheConstants.FIELD_LOGIN_TIME) instanceof String loginTimeStr)
                || !(fields.get(AuthCacheConstants.FIELD_LOGIN_IP) instanceof String loginIp)) {
            return null;
        }
        final LocalDateTime loginTime;
        try {
            loginTime = LocalDateTime.parse(loginTimeStr);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }

        var roles = this.readCodes(fields.get(AuthCacheConstants.FIELD_ROLES));
        var permissions = this.readCodes(fields.get(AuthCacheConstants.FIELD_PERMISSIONS));
        if (roles == null || permissions == null) {
            return null;
        }
        var authorization = new UserAuthorization(roles, permissions);
        var loginUser = new LoginUser(userId.longValue(), username, authorization.roles(), loginTime, loginIp);
        var authorities = authorization.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        var loginSessionId = fields.getOrDefault(AuthCacheConstants.FIELD_LOGIN_SESSION_ID, sessionId);
        if (!(loginSessionId instanceof String stableId) || stableId.isBlank()) {
            return null;
        }
        return new TokenAuthentication(sessionId, stableId, loginUser, authorities, authorization);
    }

    /**
     * 捕获 Valkey 中的用户会话索引；此处不读取或加载授权。
     * <p>
     * 批量撤销须与持久化凭据合并，避免缓存淘汰导致索引不完整时漏撤销。
     */
    public Map<Long, Set<String>> captureUserSessions(Collection<Long> userIds) {
        Map<Long, Set<String>> captured = new TreeMap<>();
        for (var userId : userIds) {
            var members = valkeyUtils.sMembers(AuthCacheConstants.USER_SESSIONS_KEY_PREFIX + userId);
            var sessions = members == null ? Set.<String>of() : members.stream()
                    .map(Object::toString).collect(java.util.stream.Collectors.toUnmodifiableSet());
            captured.put(userId, sessions);
        }
        return Collections.unmodifiableMap(captured);
    }

    /** 必须在数据库事务内注册；回滚不撤销原会话。 */
    public void revokeAfterCommit(Map<Long, Set<String>> captured) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("会话撤销必须在数据库事务内注册");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                TokenService.this.revokeCapturedSessions(captured, true);
            }
        });
    }

    /** 删除捕获的会话 Hash 和集合成员，保留并发签发的新会话。 */
    private void revokeCapturedSessions(Map<Long, Set<String>> captured, boolean databaseCommitted) {
        try {
            var keys = captured.values().stream().flatMap(Collection::stream)
                    .map(sessionId -> AuthCacheConstants.SESSION_KEY_PREFIX + sessionId)
                    .toList();
            if (!keys.isEmpty()) {
                valkeyUtils.delete(keys);
                onlineSessionIndex.remove(captured.values().stream().flatMap(Collection::stream).toList());
            }
            captured.forEach((userId, sessions) -> {
                if (!sessions.isEmpty()) {
                    valkeyUtils.sRemove(AuthCacheConstants.USER_SESSIONS_KEY_PREFIX + userId, sessions.toArray());
                }
            });
        } catch (RuntimeException failure) {
            // 不输出底层异常或原始 Token。数据库已提交时不能声称回滚。
            log.error("会话撤销失败: userIds={}, databaseCommitted={}, failureType={}",
                    captured.keySet(), databaseCommitted, failure.getClass().getSimpleName());
            throw new BusinessException((databaseCommitted ? "数据库已提交，但" : "")
                    + "会话撤销失败，请通过管理接口再次强制下线用户: " + captured.keySet());
        }
    }

    private Set<String> readCodes(Object value) {
        if (!(value instanceof Collection<?> codes) || codes.stream().anyMatch(code -> !(code instanceof String))) {
            return null;
        }
        return codes.stream().map(String.class::cast).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public UserAuthorization getAuthorization(Long userId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof TokenAuthentication tokenAuthentication
                && tokenAuthentication.getLoginUser().userId().equals(userId)) {
            return tokenAuthentication.getAuthorization();
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED, "会话缺少授权快照，请重新登录");
    }

    /**
     * 获取当前请求的权限编码
     *
     * @param userId 用户 ID
     * @return 权限编码集合
     */
    public Set<String> getPermissions(Long userId) {
        return this.getAuthorization(userId).permissions();
    }

}
