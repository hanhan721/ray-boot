package com.ray.system.service.impl;

import com.ray.common.core.constant.ResultCode;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.constant.AuthCacheConstants;
import com.ray.system.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** 使用单个 Hash 原子维护账号失败次数和冷却截止时间，窗口内换 IP 不重置计数。 */
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {
    private static final DefaultRedisScript<Long> CHECK_COOLDOWN = new DefaultRedisScript<>("""
            local blockedUntil = redis.call('HGET', KEYS[1], 'blockedUntil')
            if not blockedUntil then return 0 end
            local now = redis.call('TIME')
            local nowMillis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
            return math.max(0, math.ceil((tonumber(blockedUntil) - nowMillis) / 1000))
            """, Long.class);

    private static final DefaultRedisScript<Long> RECORD_FAILURE = new DefaultRedisScript<>("""
            local count = redis.call('HINCRBY', KEYS[1], 'count', 1)
            if redis.call('TTL', KEYS[1]) < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            local threshold = tonumber(ARGV[2])
            if count >= threshold then
                local delay = math.min(tonumber(ARGV[3]) * 2 ^ math.min(count - threshold, 30), tonumber(ARGV[4]))
                local now = redis.call('TIME')
                local nowMillis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
                redis.call('HSET', KEYS[1], 'blockedUntil', nowMillis + delay * 1000)
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties authProperties;

    @Override
    public void checkAllowed(String username) {
        var remaining = redisTemplate.execute(CHECK_COOLDOWN, List.of(this.key(username)));
        if (remaining == null) {
            throw new BusinessException("登录校验暂不可用，请稍后重试");
        }
        if (remaining > 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录失败次数过多，请" + remaining + "秒后重试");
        }
    }

    @Override
    public void recordFailure(String username) {
        var security = authProperties.getSecurity();
        var count = redisTemplate.execute(RECORD_FAILURE, List.of(this.key(username)),
                Long.toString(TimeUnit.MINUTES.toSeconds(security.getFailureWindowMinutes())),
                Integer.toString(security.getMaxLoginAttempts()),
                Integer.toString(security.getInitialCooldownSeconds()),
                Integer.toString(security.getMaxCooldownSeconds()));
        if (count == null) {
            throw new BusinessException("登录校验暂不可用，请稍后重试");
        }
    }

    @Override
    public void clearFailures(String username) {
        redisTemplate.delete(this.key(username));
    }

    private String key(String username) {
        return AuthCacheConstants.LOGIN_FAIL_KEY_PREFIX + username;
    }
}
