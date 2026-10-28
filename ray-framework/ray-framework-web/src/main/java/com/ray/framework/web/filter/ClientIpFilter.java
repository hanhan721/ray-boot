package com.ray.framework.web.filter;

import com.ray.common.core.util.IpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在限流、认证和日志处理前统一解析客户端地址。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ClientIpFilter extends OncePerRequestFilter {

    private final Set<InetAddress> trustedProxies;

    public ClientIpFilter(@Value("${ray.http.trusted-proxies:127.0.0.1,::1}") String[] trustedProxies) {
        this.trustedProxies = Arrays.stream(trustedProxies)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(InetAddress::ofLiteral)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(IpUtils.CLIENT_IP_ATTRIBUTE, IpUtils.resolveClientIp(request, trustedProxies));
        filterChain.doFilter(request, response);
    }
}
