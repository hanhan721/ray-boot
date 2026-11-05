package com.ray.system.file;

import com.ray.common.core.domain.R;
import com.ray.common.storage.config.StorageProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.concurrent.Semaphore;

/** 在 Servlet 解析 multipart 之前限制请求，避免临时文件先于 Service 限流无限堆积。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class FileUploadAdmissionFilter extends OncePerRequestFilter {
    private final Semaphore requests;
    private final JsonMapper json;
    public FileUploadAdmissionFilter(StorageProperties properties, JsonMapper json) {
        this.requests = new Semaphore(properties.uploadConcurrency());
        this.json = json;
    }

    /** 只拦截单文件与批量上传入口，路径判断不读取请求体或触发 multipart 解析。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod())
                || !("/api/sys/file".equals(path) || "/api/sys/file/batch".equals(path));
    }

    /** 解析 multipart 前申请请求名额；超限立即响应，处理结束或异常时均释放名额。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 1.非阻塞申请名额，超限直接返回 429，避免等待期间继续堆积临时文件。
        if (!requests.tryAcquire()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", "1");
            response.getWriter().write(json.writeValueAsString(R.fail("上传并发已满，请稍后重试")));
            return;
        }
        // 2.放行到认证和 MVC 处理链，最后统一释放名额，覆盖处理异常的情况。
        try { chain.doFilter(request, response); }
        finally { requests.release(); }
    }
}
