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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Refresh Token Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl extends ServiceImpl<RefreshTokenMapper, RefreshToken> implements RefreshTokenService {

    private final AuthProperties authProperties;

    @Override
    public void create(String rawToken, Long userId, String accessToken, String userAgent, String ip) {
        var tokenHash = this.hashToken(rawToken);
        var expireTime = LocalDateTime.now().plusDays(authProperties.getToken().getRefreshExpire());

        var refreshToken = new RefreshToken()
                .setTokenHash(tokenHash)
                .setUserId(userId)
                .setAccessToken(accessToken)
                .setExpireTime(expireTime)
                .setUserAgent(userAgent)
                .setIp(ip);

        super.save(refreshToken);
    }

    @Override
    public RefreshToken verify(String rawToken) {
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
