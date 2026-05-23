package com.ray.framework.log.aspect;

import com.ray.framework.log.annotation.OperateLog;
import com.ray.framework.log.event.OperateLogEvent;
import com.ray.framework.log.handler.OperateLogHandler;
import com.ray.framework.log.util.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperateLogAspect {

    private final OperateLogHandler operateLogHandler;
    private final JsonMapper jsonMapper;

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperateLog operateLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperateLogEvent event = new OperateLogEvent();

        // 设置基础信息
        event.setModule(operateLog.module())
                .setDescription(operateLog.description())
                .setOperateType(operateLog.type())
                .setOperateTime(LocalDateTime.now());

        // 设置方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        event.setMethodName(className + "." + methodName + "()");

        // 设置请求信息
        HttpServletRequest request = LogUtils.getRequest();
        if (request != null) {
            event.setRequestMethod(request.getMethod());
            event.setRequestUrl(request.getRequestURI());
            event.setIp(LogUtils.getClientIp(request));
        }

        // 记录请求参数
        if (operateLog.saveRequestData()) {
            event.setRequestParams(getRequestParams(joinPoint));
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            event.setStatus(1);

            // 记录响应数据
            if (operateLog.saveResponseData() && result != null) {
                event.setResponseData(truncate(toJson(result), 2000));
            }
        } catch (Throwable e) {
            event.setStatus(0);
            event.setErrorMsg(truncate(e.getMessage(), 500));
            throw e;
        } finally {
            event.setDuration(System.currentTimeMillis() - startTime);

            // 异步处理日志
            try {
                operateLogHandler.handle(event);
            } catch (Exception e) {
                log.error("操作日志记录失败", e);
            }
        }

        return result;
    }

    private String getRequestParams(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || paramNames.length == 0) {
            return null;
        }

        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            Object arg = args[i];
            // 过滤特殊类型
            if (arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile) {
                continue;
            }
            params.put(paramNames[i], arg);
        }

        return truncate(toJson(params), 2000);
    }

    private String toJson(Object obj) {
        try {
            return jsonMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
