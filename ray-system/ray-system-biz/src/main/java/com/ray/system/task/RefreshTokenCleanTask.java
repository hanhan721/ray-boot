package com.ray.system.task;

import com.ray.common.core.annotation.ScheduleTask;
import com.ray.system.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 定时清理任务
 */
@Slf4j
@Component
@ScheduleTask
@RequiredArgsConstructor
public class RefreshTokenCleanTask {

    private final RefreshTokenService refreshTokenService;

    /**
     * 清理过期的 Refresh Token
     */
    public void cleanExpiredTokens() {
        log.info("开始清理过期 Refresh Token...");
        var count = refreshTokenService.cleanExpired();
        log.info("清理过期 Refresh Token 完成，共清理 {} 条", count);
    }
}
