package com.ray.framework.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 接口访问日志过滤器
 * <p>
 * 请求进入时打印请求日志（方法、路径、请求体），
 * 请求返回时打印响应日志（状态码、耗时、响应体）。
 * 仅拦截 /api/ 开头的请求，超长内容自动截断。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_LENGTH = 500;
    private static final int MAX_CACHEABLE_BODY_SIZE = 100 * 1024; // 100KB

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        var method = request.getMethod();
        var path = buildPath(request);
        var contentType = request.getContentType();

        // multipart 请求或超大请求体不读取 body
        boolean isMultipart = contentType != null && contentType.startsWith("multipart/");
        boolean isTooLarge = request.getContentLengthLong() > MAX_CACHEABLE_BODY_SIZE;

        HttpServletRequest wrappedRequest;
        if (isMultipart) {
            log.info("--> {} {} | body: [multipart/form-data]", method, path);
            wrappedRequest = request;
        } else if (isTooLarge) {
            log.info("--> {} {} | body: [too large: {}KB]", method, path, request.getContentLengthLong() / 1024);
            wrappedRequest = request;
        } else {
            byte[] requestBody = request.getInputStream().readAllBytes();
            log.info("--> {} {} | body: {}", method, path, truncate(bodyToString(requestBody)));
            wrappedRequest = new CachedBodyRequestWrapper(request, requestBody);
        }

        var cachedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, cachedResponse);
        } finally {
            // 返回时打印响应日志
            long duration = System.currentTimeMillis() - startTime;
            var responseBody = bodyToString(cachedResponse.getContentAsByteArray());
            log.info("<-- {} {} {} {}ms | body: {}", method, path, cachedResponse.getStatus(), duration, truncate(responseBody));
            cachedResponse.copyBodyToResponse();
        }
    }

    private String buildPath(HttpServletRequest request) {
        var uri = request.getRequestURI();
        var query = request.getQueryString();
        return StringUtils.hasText(query) ? uri + "?" + query : uri;
    }

    private String bodyToString(byte[] content) {
        if (content == null || content.length == 0) {
            return "-";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String truncate(String str) {
        if (str.length() <= MAX_BODY_LENGTH) {
            return str;
        }
        return str.substring(0, MAX_BODY_LENGTH) + "...(truncated)";
    }

    /**
     * 缓存请求体的 Request 包装器，允许下游重复读取 body
     */
    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedServletInputStream(this.body);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        CachedServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            // no-op
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
