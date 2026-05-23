package com.ray.framework.log.util;

import com.ray.common.core.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 日志工具类
 */
public final class LogUtils {

    private LogUtils() {
    }

    /**
     * 获取当前请求
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端 IP
     */
    public static String getClientIp() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "unknown";
        }
        return getClientIp(request);
    }

    /**
     * 获取客户端 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        return IpUtils.getClientIp(request);
    }
}
