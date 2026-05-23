package com.ray.common.valkey.config;

import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientOptionsBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Valkey 配置
 */
@Configuration
@EnableCaching
public class ValkeyConfig {

    /**
     * Lettuce 客户端调优，解决远程连接空闲后被防火墙/NAT 静默断开的问题
     * <p>
     * 使用 Spring Boot 4 提供的 {@link LettuceClientOptionsBuilderCustomizer} 扩展点，
     * 基于现有传递依赖，无需额外引入新依赖。
     * 兼容 Redis 7.x 和 Valkey 9.x（均使用 RESP 协议）
     */
    @Bean
    public LettuceClientOptionsBuilderCustomizer lettuceClientOptionsCustomizer() {
        return builder -> builder
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .keepAlive(SocketOptions.KeepAliveOptions.builder()
                                .enable()
                                .idle(Duration.ofSeconds(15))
                                .interval(Duration.ofSeconds(5))
                                .count(3)
                                .build())
                        .build())
                .timeoutOptions(TimeoutOptions.enabled());
    }

    @Bean
    public GenericJacksonJsonRedisSerializer jsonRedisSerializer() {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubTypeIsArray()
                .allowIfSubType("com.ray.")
                .allowIfSubType("java.util.")
                .build();

        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .writer((mapper, source) -> mapper.writeValueAsBytes(toMutable(source)))
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       GenericJacksonJsonRedisSerializer jsonSerializer) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);

        var stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     GenericJacksonJsonRedisSerializer jsonSerializer) {
        var config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // 系统参数缓存：无过期时间，通过业务逻辑手动失效
        var paramConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("sysParam", paramConfig)
                .build();
    }

    /**
     * 将不可变集合转为可变集合，解决 Jackson NON_FINAL 模式下
     * List.of() / Set.of() / Map.of() 等 final 类型无法写入类型信息的问题
     */
    private static Object toMutable(Object value) {
        return switch (value) {
            case null -> null;
            case List<?> list -> new ArrayList<>(list);
            case Set<?> set -> new LinkedHashSet<>(set);
            case Map<?, ?> map -> new LinkedHashMap<>(map);
            default -> value;
        };
    }
}
