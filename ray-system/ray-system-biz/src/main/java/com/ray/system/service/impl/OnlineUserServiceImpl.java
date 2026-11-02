package com.ray.system.service.impl;

import com.ray.common.security.constant.AuthCacheConstants;
import com.ray.common.security.service.TokenService;
import com.ray.common.valkey.util.ValkeyUtils;
import com.ray.system.api.domain.OnlineUserVO;
import com.ray.system.security.AuthorizationSessionCoordinator;
import com.ray.system.service.OnlineUserService;
import com.ray.system.service.RefreshTokenService;
import com.ray.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 在线用户 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private final ValkeyUtils valkeyUtils;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final AuthorizationSessionCoordinator sessionCoordinator;

    @Override
    public List<OnlineUserVO> list(String username, String loginIp) {
        // 1. 扫描尚未过期的 Token Hash，包含供管理员踢除的旧会话。
        var tokenKeys = valkeyUtils.scan(AuthCacheConstants.TOKEN_KEY_PREFIX + "*", 200);
        if (CollectionUtils.isEmpty(tokenKeys)) {
            return List.of();
        }

        // 2. Pipeline 批量 HGETALL，一次网络往返获取所有 token 详情
        var keyList = tokenKeys.stream().sorted().toList();
        var fieldsList = valkeyUtils.pipelinedHGetAll(keyList);

        // 3. 组装 VO 并筛选
        var prefixLen = AuthCacheConstants.TOKEN_KEY_PREFIX.length();
        return IntStream.range(0, keyList.size())
                .mapToObj(i -> {
                    var fields = fieldsList.get(i);
                    if (CollectionUtils.isEmpty(fields)) {
                        return null;
                    }
                    var uuid = keyList.get(i).substring(prefixLen);
                    var sessionId = (String) fields.get(AuthCacheConstants.FIELD_SESSION_ID);
                    if (!StringUtils.hasText(sessionId)) {
                        // 补充旧会话的管理索引，不恢复其认证能力。
                        sessionId = tokenService.registerSession(uuid);
                    }
                    return sessionId != null ? this.buildOnlineUserVO(sessionId, fields) : null;
                })
                .filter(Objects::nonNull)
                .filter(vo -> !StringUtils.hasText(username)
                        || vo.username().toLowerCase().contains(username.toLowerCase()))
                .filter(vo -> !StringUtils.hasText(loginIp)
                        || vo.loginIp().contains(loginIp))
                .sorted(Comparator.comparing(OnlineUserVO::loginTime).reversed())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickByToken(String tokenId) {
        sessionCoordinator.lockForChange();
        var accessToken = tokenService.getAccessTokenBySessionId(tokenId);
        if (accessToken == null) {
            return;
        }
        var tokenKey = AuthCacheConstants.TOKEN_KEY_PREFIX + accessToken;
        var fields = valkeyUtils.hGetAll(tokenKey);

        if (!CollectionUtils.isEmpty(fields)) {
            var userId = ((Number) fields.get(AuthCacheConstants.FIELD_USER_ID)).longValue();
            // 与续期共用用户行锁，使等待中的刷新在提交后重新验证到失效状态。
            userService.getByIdForUpdate(userId);
            refreshTokenService.deleteByAccessToken(accessToken);
            tokenService.revokeAfterCommit(Map.of(userId, Set.of(accessToken)));
            log.info("强制下线单个会话: userId={}", userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickByUserId(Long userId) {
        sessionCoordinator.lockForChange();
        sessionCoordinator.revokeUsers(Set.of(userId));

        log.info("强制下线用户所有会话: userId={}", userId);
    }

    private OnlineUserVO buildOnlineUserVO(String sessionId, Map<Object, Object> fields) {
        try {
            var userId = ((Number) fields.get(AuthCacheConstants.FIELD_USER_ID)).longValue();
            var tokenUsername = (String) fields.get(AuthCacheConstants.FIELD_USERNAME);
            var nickname = (String) fields.getOrDefault(AuthCacheConstants.FIELD_NICKNAME, "");
            var loginIp = (String) fields.get(AuthCacheConstants.FIELD_LOGIN_IP);
            var browser = (String) fields.getOrDefault(AuthCacheConstants.FIELD_BROWSER, "Unknown");
            var os = (String) fields.getOrDefault(AuthCacheConstants.FIELD_OS, "Unknown");
            var loginTimeStr = (String) fields.get(AuthCacheConstants.FIELD_LOGIN_TIME);
            var loginTime = LocalDateTime.parse(loginTimeStr);

            return new OnlineUserVO(sessionId, userId, tokenUsername, nickname, loginIp, browser, os, loginTime);
        } catch (Exception e) {
            log.warn("解析在线用户信息失败");
            return null;
        }
    }
}
