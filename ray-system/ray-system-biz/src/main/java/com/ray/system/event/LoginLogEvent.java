package com.ray.system.event;

import java.time.LocalDateTime;

/**
 * 登录日志事件
 *
 * @param username  用户名
 * @param loginIp   登录 IP
 * @param userAgent User-Agent
 * @param success   是否成功
 * @param msg       提示消息
 * @param loginTime 登录时间
 */
public record LoginLogEvent(
        String username,
        String loginIp,
        String userAgent,
        boolean success,
        String msg,
        LocalDateTime loginTime
) {
}
