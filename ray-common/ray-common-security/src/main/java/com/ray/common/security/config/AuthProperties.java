package com.ray.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 认证配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "ray.auth")
public class AuthProperties {

    private TokenProperties token = new TokenProperties();

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

        /**
         * 登录失败锁定次数
         */
        private int maxLoginAttempts = 5;

        /**
         * 登录失败锁定时间（分钟）
         */
        private int lockDuration = 30;
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
