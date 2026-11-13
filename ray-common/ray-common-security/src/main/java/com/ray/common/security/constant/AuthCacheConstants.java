package com.ray.common.security.constant;

/**
 * 认证缓存 Key 常量
 */
public final class AuthCacheConstants {

    private AuthCacheConstants() {
    }

    /**
     * 会话主体 (Hash)：session:{SHA-256(Access Token)}。
     * 保存身份及授权快照，摘要同时用作管理会话标识。
     */
    public static final String SESSION_KEY_PREFIX = "session:";

    /**
     * 用户会话索引 (Set)：user_sessions:{userId} -> sessionId 集合。
     * <p>
     * 仅作批量撤销的快速路径；持久化来源为 sys_refresh_token，索引被缓存淘汰时不得漏撤销。
     */
    public static final String USER_SESSIONS_KEY_PREFIX = "user_sessions:";

    /**
     * 登录失败状态 (Hash)：count、blockedUntil
     * <p>格式: login_fail:{username}
     */
    public static final String LOGIN_FAIL_KEY_PREFIX = "login_fail:";

    // ==================== Hash Field 常量 ====================

    public static final String FIELD_USER_ID = "userId";
    /** 一次登录的稳定标识，刷新时保持不变。 */
    public static final String FIELD_LOGIN_SESSION_ID = "loginSessionId";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_NICKNAME = "nickname";
    public static final String FIELD_ROLES = "roles";
    public static final String FIELD_PERMISSIONS = "permissions";
    public static final String FIELD_LOGIN_TIME = "loginTime";
    public static final String FIELD_LOGIN_IP = "loginIp";
    public static final String FIELD_BROWSER = "browser";
    public static final String FIELD_OS = "os";
}
