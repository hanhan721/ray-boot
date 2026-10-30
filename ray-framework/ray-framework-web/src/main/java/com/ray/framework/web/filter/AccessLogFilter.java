package com.ray.framework.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 接口访问日志：仅记录方法、路径、状态与耗时，避免请求、响应及查询串中的凭据进入日志。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AccessLogFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        var method = request.getMethod();
        var path = request.getRequestURI();
        // 兼容仍持有旧在线列表的客户端，防止旧版凭据被放入踢下线路径后进入日志。
        if (path.startsWith("/api/sys/online/")
                && !path.substring("/api/sys/online/".length()).contains("/")
                && !path.equals("/api/sys/online/list")) {
            path = "/api/sys/online/{tokenId}";
        }
        log.info("--> {} {}", method, path);

        long startTime = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            log.info("<-- {} {} {} {}ms", method, path, response.getStatus(), duration);
        }
    }
}
