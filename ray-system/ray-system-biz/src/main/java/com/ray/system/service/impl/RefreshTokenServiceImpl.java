package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.security.config.AuthProperties;
import com.ray.system.api.entity.RefreshToken;
import com.ray.system.mapper.RefreshTokenMapper;
import com.ray.system.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Refresh Token Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl extends ServiceImpl<RefreshTokenMapper, RefreshToken> implements RefreshTokenService {

    private final AuthProperties authProperties;

    @Override
    public void create(String rawToken, Long userId, String sessionId, String userAgent, String ip) {
        this.create(rawToken, userId, sessionId, sessionId, userAgent, ip);
    }

    @Override
    public void create(String rawToken, Long userId, String sessionId, String loginSessionId, String userAgent, String ip) {
        var tokenHash = this.hashToken(rawToken);
        var expireTime = LocalDateTime.now().plusDays(authProperties.getToken().getRefreshExpire());

        var refreshToken = new RefreshToken()
                .setTokenHash(tokenHash)
                .setUserId(userId)
                .setSessionId(sessionId)
                .setLoginSessionId(loginSessionId)
                .setExpireTime(expireTime)
                .setUserAgent(userAgent)
                .setIp(ip);

        super.save(refreshToken);
    }

    @Override
    public RefreshToken verify(String rawToken) {
        var token = this.findForLogout(rawToken);
        return token != null && !Boolean.TRUE.equals(token.getRotated()) ? token : null;
    }

    @Override
    public RefreshToken findForLogout(String rawToken) {
        var tokenHash = this.hashToken(rawToken);

        var refreshToken = super.getOne(
                Wrappers.<RefreshToken>lambdaQuery()
                        .eq(RefreshToken::getTokenHash, tokenHash)
        );

        if (refreshToken == null) {
            return null;
        }

        // 检查是否过期
        if (refreshToken.getExpireTime().isBefore(LocalDateTime.now())) {
            super.removeById(refreshToken.getId());
            return null;
        }

        return refreshToken;
    }

    @Override
    public void markRotated(String tokenHash) {
        super.update(Wrappers.<RefreshToken>lambdaUpdate()
                .eq(RefreshToken::getTokenHash, tokenHash).set(RefreshToken::getRotated, true));
    }

    @Override
    public Set<String> revokeLoginSession(Long userId, String loginSessionId) {
        var sessions = super.list(Wrappers.<RefreshToken>lambdaQuery()
                        .eq(RefreshToken::getUserId, userId)
                        .eq(RefreshToken::getLoginSessionId, loginSessionId)
                        .eq(RefreshToken::getRotated, false)).stream()
                .map(RefreshToken::getSessionId).collect(Collectors.toSet());
        super.remove(Wrappers.<RefreshToken>lambdaQuery()
                .eq(RefreshToken::getUserId, userId).eq(RefreshToken::getLoginSessionId, loginSessionId));
        return sessions;
    }

    @Override
    public void deleteByTokenHash(String tokenHash) {
        super.remove(
                Wrappers.<RefreshToken>lambdaQuery()
                        .eq(RefreshToken::getTokenHash, tokenHash)
        );
    }

    @Override
    public void deleteByUserId(Long userId) {
        super.remove(
                Wrappers.<RefreshToken>lambdaQuery()
                        .eq(RefreshToken::getUserId, userId)
        );
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        super.remove(
                Wrappers.<RefreshToken>lambdaQuery()
                        .eq(RefreshToken::getSessionId, sessionId)
        );
    }

    @Override
    @Transactional(timeout = 15)
    public int cleanExpired() {
        var wrapper = Wrappers.<RefreshToken>lambdaQuery()
                .lt(RefreshToken::getExpireTime, LocalDateTime.now());

        long count = super.count(wrapper);
        if (count > 0) {
            super.remove(wrapper);
            log.info("清理过期 Refresh Token: {} 条", count);
        }
        return (int) count;
    }

    /**
     * 对原始 Token 进行 SHA-256 哈希
     */
    private String hashToken(String rawToken) {
        try {
            var salt = authProperties.getToken().getRefreshSalt();
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update((rawToken + salt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
