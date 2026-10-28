package com.ray.common.core.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.util.Set;

/**
 * IP 地址工具类
 */
public final class IpUtils {

    public static final String CLIENT_IP_ATTRIBUTE = IpUtils.class.getName() + ".clientIp";

    private IpUtils() {
    }

    /**
     * 获取入口过滤器解析的客户端 IP；未经过过滤器时只使用连接地址。
     */
    public static String getClientIp(HttpServletRequest request) {
        var resolved = request.getAttribute(CLIENT_IP_ATTRIBUTE);
        return resolved instanceof String ip ? ip : request.getRemoteAddr();
    }

    /**
     * 从连接地址开始，自右向左剥离明确受信的代理，停在第一个非受信地址。
     * 代理须追加实际连接地址或覆盖 XFF；不读取可能由客户端原样透传的其他 IP 头。
     */
    public static String resolveClientIp(HttpServletRequest request, Set<InetAddress> trustedProxies) {
        var remoteAddr = request.getRemoteAddr();
        var current = parseAddress(remoteAddr);
        if (current == null || !trustedProxies.contains(current)) {
            return remoteAddr;
        }

        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddr;
        }

        var hops = forwardedFor.split(",", -1);
        for (int index = hops.length - 1; index >= 0 && trustedProxies.contains(current); index--) {
            var hop = parseAddress(hops[index]);
            if (hop == null) {
                return remoteAddr;
            }
            current = hop;
        }
        return current.getHostAddress();
    }

    private static InetAddress parseAddress(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            // 只解析 IP 字面量，不对不可信的头部执行 DNS 查询。
            return InetAddress.ofLiteral(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
