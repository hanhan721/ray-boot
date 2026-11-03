package com.ray.common.security.service;

import com.ray.common.core.exception.BusinessException;
import com.ray.common.security.constant.AuthCacheConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

/** 在线管理索引；独立保存排序时间和过期时间，列表读取及过期清理均有数量上限。 */
@Component
@RequiredArgsConstructor
public class OnlineSessionIndex {
    private static final List<String> KEYS = List.of("online_sessions:time", "online_sessions:expiry");
    private static final DefaultRedisScript<Long> ADD = new DefaultRedisScript<>("""
            local ttl = redis.call('PTTL', KEYS[3])
            if ttl <= 0 then return 0 end
            local clock = redis.call('TIME')
            local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
            redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
            redis.call('ZADD', KEYS[2], now + ttl, ARGV[1])
            return 1
            """, Long.class);
    private static final String CLEANUP = """
            local clock = redis.call('TIME')
            local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
            local expired = redis.call('ZRANGEBYSCORE', KEYS[2], '-inf', now, 'LIMIT', 0, 1000)
            for _, id in ipairs(expired) do
                redis.call('ZREM', KEYS[1], id)
                redis.call('ZREM', KEYS[2], id)
            end
            """;
    private static final DefaultRedisScript<Long> CLEAN_EXPIRED = new DefaultRedisScript<>(
            CLEANUP + "return #expired", Long.class);
    private static final DefaultRedisScript<List> PAGE = new DefaultRedisScript<>(CLEANUP + """
            if redis.call('ZCOUNT', KEYS[2], '-inf', now) > 0 then return {-1} end
            local total = redis.call('ZCOUNT', KEYS[1], ARGV[1], ARGV[2])
            local offset = tonumber(ARGV[3])
            if offset >= total then return {total} end
            local newer = 0
            if ARGV[2] ~= '+inf' then
                newer = redis.call('ZCOUNT', KEYS[1], '(' .. ARGV[2], '+inf')
            end
            local size = math.min(tonumber(ARGV[4]), total - offset)
            local ids = redis.call('ZRANGE', KEYS[1], newer + offset, newer + offset + size - 1, 'REV')
            local result = {total}
            for _, id in ipairs(ids) do table.insert(result, id) end
            return result
            """, List.class);
    private static final DefaultRedisScript<Long> REMOVE = new DefaultRedisScript<>("""
            for _, id in ipairs(ARGV) do
                redis.call('ZREM', KEYS[1], id)
                redis.call('ZREM', KEYS[2], id)
            end
            return #ARGV
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public void add(String sessionId, LocalDateTime loginTime) {
        var result = redisTemplate.execute(ADD,
                List.of(KEYS.getFirst(), KEYS.getLast(), AuthCacheConstants.SESSION_KEY_PREFIX + sessionId),
                sessionId, Long.toString(loginTime.toInstant(ZoneOffset.UTC).toEpochMilli()));
        if (result == null) {
            throw new BusinessException("在线会话索引暂不可用");
        }
    }

    public void remove(Collection<String> sessionIds) {
        if (!sessionIds.isEmpty()) {
            redisTemplate.execute(REMOVE, KEYS, sessionIds.toArray());
        }
    }

    public SessionPage page(LocalDateTime startTime, LocalDateTime endTime, long offset, int size) {
        var result = redisTemplate.execute(PAGE, KEYS,
                startTime == null ? "-inf" : Long.toString(startTime.toInstant(ZoneOffset.UTC).toEpochMilli()),
                endTime == null ? "+inf" : Long.toString(endTime.toInstant(ZoneOffset.UTC).toEpochMilli()),
                Long.toString(offset), Integer.toString(size));
        if (result == null || result.isEmpty()) {
            throw new BusinessException("在线会话索引暂不可用");
        }
        var total = ((Number) result.getFirst()).longValue();
        if (total < 0) {
            throw new BusinessException("在线会话索引正在清理，请稍后重试");
        }
        return new SessionPage(total, result.subList(1, result.size()).stream().map(Object::toString).toList());
    }

    /** 每次最多清理 1000 条，避免长时间阻塞 Valkey。 */
    @Scheduled(fixedDelay = 1000)
    public void cleanExpired() {
        redisTemplate.execute(CLEAN_EXPIRED, KEYS);
    }

    public record SessionPage(long total, List<String> sessionIds) { }
}
