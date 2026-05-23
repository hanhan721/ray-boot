package com.ray.system.service.impl;

import com.ray.common.security.constant.AuthCacheConstants;
import com.ray.common.security.service.TokenService;
import com.ray.common.valkey.util.ValkeyUtils;
import com.ray.system.api.domain.OnlineUserVO;
import com.ray.system.service.OnlineUserService;
import com.ray.system.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Override
    public List<OnlineUserVO> list(String username, String loginIp) {
        // 1. SCAN 直接扫描所有 token:* 获取全部活跃 token key
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
                    return this.buildOnlineUserVO(uuid, fields);
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
    public void kickByToken(String tokenId) {
        var tokenKey = AuthCacheConstants.TOKEN_KEY_PREFIX + tokenId;
        var fields = valkeyUtils.hGetAll(tokenKey);

        if (!CollectionUtils.isEmpty(fields)) {
            var userId = ((Number) fields.get(AuthCacheConstants.FIELD_USER_ID)).longValue();
            tokenService.removeAccessToken(tokenId, userId);
            log.info("强制下线单个会话: tokenId={}, userId={}", tokenId, userId);
        }
    }

    @Override
    public void kickByUserId(Long userId) {
        tokenService.removeAllTokens(userId);
        refreshTokenService.deleteByUserId(userId);
        tokenService.removeMenus(userId);
        tokenService.removePermissions(userId);

        log.info("强制下线用户所有会话: userId={}", userId);
    }

    private OnlineUserVO buildOnlineUserVO(String uuid, Map<Object, Object> fields) {
        try {
            var userId = ((Number) fields.get(AuthCacheConstants.FIELD_USER_ID)).longValue();
            var tokenUsername = (String) fields.get(AuthCacheConstants.FIELD_USERNAME);
            var nickname = (String) fields.getOrDefault(AuthCacheConstants.FIELD_NICKNAME, "");
            var loginIp = (String) fields.get(AuthCacheConstants.FIELD_LOGIN_IP);
            var browser = (String) fields.getOrDefault(AuthCacheConstants.FIELD_BROWSER, "Unknown");
            var os = (String) fields.getOrDefault(AuthCacheConstants.FIELD_OS, "Unknown");
            var loginTimeStr = (String) fields.get(AuthCacheConstants.FIELD_LOGIN_TIME);
            var loginTime = LocalDateTime.parse(loginTimeStr);

            return new OnlineUserVO(uuid, userId, tokenUsername, nickname, loginIp, browser, os, loginTime);
        } catch (Exception e) {
            log.warn("解析在线用户信息失败: tokenId={}", uuid, e);
            return null;
        }
    }
}
