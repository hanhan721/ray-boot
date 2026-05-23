package com.ray.common.core.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 地址工具类
 */
public final class IpUtils {

    private IpUtils() {
    }

    /**
     * 获取客户端真实 IP，依次检查常见代理头部
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // 多级代理时取第一个
            int index = ip.indexOf(',');
            return index != -1 ? ip.substring(0, index).trim() : ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }
}
