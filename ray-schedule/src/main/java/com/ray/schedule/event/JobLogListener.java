package com.ray.schedule.event;

import com.ray.schedule.mapper.JobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 任务执行日志事件监听器（异步）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobLogListener {

    private final JobLogMapper jobLogMapper;

    @Async
    @EventListener
    public void onJobLog(JobLogEvent event) {
        try {
            jobLogMapper.insert(event.jobLog());
        } catch (Exception e) {
            log.error("记录任务执行日志失败: {}", e.getMessage(), e);
        }
    }
}
