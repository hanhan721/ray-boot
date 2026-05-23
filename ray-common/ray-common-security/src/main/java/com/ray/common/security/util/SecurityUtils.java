package com.ray.common.security.util;

import com.ray.common.security.domain.LoginUser;
import com.ray.common.security.domain.TokenAuthentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

/**
 * Security 工具类
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户
     *
     * @return 登录用户信息，未登录返回 null
     */
    public static LoginUser getLoginUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof TokenAuthentication tokenAuth) {
            return tokenAuth.getLoginUser();
        }
        return null;
    }

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID，未登录返回 null
     */
    public static Long getUserId() {
        var loginUser = getLoginUser();
        return loginUser != null ? loginUser.userId() : null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，未登录返回 null
     */
    public static String getUsername() {
        var loginUser = getLoginUser();
        return loginUser != null ? loginUser.username() : null;
    }

    /**
     * 获取当前请求的 Access Token
     *
     * @return Access Token UUID，未登录返回 null
     */
    public static String getAccessToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof TokenAuthentication tokenAuth) {
            return tokenAuth.getAccessToken();
        }
        return null;
    }

    /**
     * 获取当前用户角色集合
     *
     * @return 用户角色集合，未登录返回 null
     */
    public static Set<String> getRoles() {
        var loginUser = getLoginUser();
        return loginUser != null ? loginUser.roles() : null;
    }
}
