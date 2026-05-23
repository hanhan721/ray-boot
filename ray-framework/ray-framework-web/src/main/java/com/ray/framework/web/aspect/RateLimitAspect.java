package com.ray.framework.web.aspect;

import com.ray.common.core.exception.BusinessException;
import com.ray.common.core.util.IpUtils;
import com.ray.framework.web.annotation.RateLimit;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * IP 限流切面
 * <p>
 * 基于 Valkey 固定窗口计数器实现，配合 {@link RateLimit} 注解使用
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "rate_limit:";

    /**
     * Lua 脚本：原子执行 INCR + EXPIRE，避免竞态条件
     * <p>
     * KEYS[1] = 限流 key，ARGV[1] = 窗口时间（秒）
     * 返回当前计数
     */
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        var request = attributes.getRequest();
        var ip = IpUtils.getClientIp(request);
        var key = StringUtils.hasText(rateLimit.key()) ? rateLimit.key() : request.getRequestURI();
        var cacheKey = KEY_PREFIX + ip + ":" + key;

        var count = redisTemplate.execute(RATE_LIMIT_SCRIPT, List.of(cacheKey), String.valueOf(rateLimit.window()));

        if (count != null && count > rateLimit.max()) {
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

}
