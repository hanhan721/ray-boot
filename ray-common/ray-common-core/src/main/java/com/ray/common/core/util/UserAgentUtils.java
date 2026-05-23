package com.ray.common.core.util;

import org.springframework.util.StringUtils;

/**
 * User-Agent 解析工具类
 */
public final class UserAgentUtils {

    private UserAgentUtils() {
    }

    /**
     * 从 User-Agent 提取浏览器信息
     */
    public static String parseBrowser(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
            return "Chrome";
        } else if (userAgent.contains("Edg")) {
            return "Edge";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            return "Safari";
        }
        return "Other";
    }

    /**
     * 从 User-Agent 提取操作系统信息
     */
    public static String parseOs(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Mac OS")) {
            return "macOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iOS") || userAgent.contains("iPhone")) {
            return "iOS";
        }
        return "Other";
    }
}
