package com.ray.common.security.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.Set;

/**
 * 认证配置属性
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "ray.auth")
public class AuthProperties {

    private TokenProperties token = new TokenProperties();

    @Valid
    private SecurityProperties security = new SecurityProperties();

    private SuperAdminProperties superAdmin = new SuperAdminProperties();

    /**
     * Token 配置
     */
    @Data
    public static class TokenProperties {

        /**
         * Access Token 过期时间（分钟）
         */
        private int accessExpire = 30;

        /**
         * Refresh Token 过期时间（天）
         */
        private int refreshExpire = 7;

        /**
         * Refresh Token 哈希盐值
         */
        private String refreshSalt = "ray-refresh-token-salt-change-in-production";
    }

    /**
     * 安全配置
     */
    @Data
    public static class SecurityProperties {

        /** 开始冷却的账号累计失败次数。 */
        @Min(1)
        private int maxLoginAttempts = 5;

        /** 失败次数累计窗口（分钟），后续失败不延长窗口。 */
        @Min(1)
        private int failureWindowMinutes = 30;

        /** 达到阈值时的初始冷却时间（秒）。 */
        @Min(1)
        private int initialCooldownSeconds = 1;

        /** 指数退避的冷却上限（秒）。 */
        @Min(1)
        private int maxCooldownSeconds = 30;
    }

    /**
     * 超管配置
     */
    @Data
    public static class SuperAdminProperties {

        /**
         * 超管角色代码集合，拥有最高权限，可访问任何接口和菜单
         * 示例: ["admin", "super-admin"]
         */
        private Set<String> roleCodes = new HashSet<>();
    }
}
