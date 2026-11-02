package com.ray.common.security.constant;

/**
 * 认证缓存 Key 常量
 */
public final class AuthCacheConstants {

    private AuthCacheConstants() {
    }

    /**
     * Access Token 主体 (Hash)
     * <p>格式: token:{uuid}
     * <p>Fields: userId, username, roles, permissions, loginTime, loginIp
     */
    public static final String TOKEN_KEY_PREFIX = "token:";

    /**
     * 管理会话索引 (String)：session:{SHA-256(Access Token)} -> Access Token。
     * 与关联 Access Token 同时过期；对外仅暴露摘要，不能作为认证凭据。
     */
    public static final String SESSION_KEY_PREFIX = "session:";

    /**
     * 用户 Token 索引 (Set)
     * <p>格式: user_tokens:{userId}
     * <p>Members: Access Token UUID 集合
     */
    public static final String USER_TOKENS_KEY_PREFIX = "user_tokens:";

    /**
     * 登录失败计数 (String)
     * <p>格式: login_fail:{username}
     */
    public static final String LOGIN_FAIL_KEY_PREFIX = "login_fail:";

    // ==================== Hash Field 常量 ====================

    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_NICKNAME = "nickname";
    public static final String FIELD_ROLES = "roles";
    public static final String FIELD_PERMISSIONS = "permissions";
    public static final String FIELD_LOGIN_TIME = "loginTime";
    public static final String FIELD_LOGIN_IP = "loginIp";
    public static final String FIELD_BROWSER = "browser";
    public static final String FIELD_OS = "os";
    public static final String FIELD_SESSION_ID = "sessionId";
}
