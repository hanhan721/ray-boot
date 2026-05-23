package com.ray.system.event;

import com.ray.common.core.util.UserAgentUtils;
import com.ray.system.api.entity.SysLoginLog;
import com.ray.system.mapper.LoginLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 登录日志事件监听器（异步）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogListener {

    private final LoginLogMapper loginLogMapper;

    @Async
    @EventListener
    public void onLoginLog(LoginLogEvent event) {
        try {
            var loginLog = new SysLoginLog()
                    .setUsername(event.username())
                    .setLoginIp(event.loginIp())
                    .setBrowser(UserAgentUtils.parseBrowser(event.userAgent()))
                    .setOs(UserAgentUtils.parseOs(event.userAgent()))
                    .setSuccess(event.success())
                    .setMsg(event.msg())
                    .setLoginTime(event.loginTime());

            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志失败: {}", e.getMessage(), e);
        }
    }
}
