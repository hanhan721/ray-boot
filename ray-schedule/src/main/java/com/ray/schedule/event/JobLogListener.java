package com.ray.schedule.event;

import com.ray.schedule.mapper.JobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 在任务线程中写完执行日志，使调度停机等待覆盖日志收尾。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobLogListener {

    private final JobLogMapper jobLogMapper;

    @EventListener
    public void onJobLog(JobLogEvent event) {
        try {
            jobLogMapper.insert(event.jobLog());
        } catch (Exception e) {
            log.error("记录任务执行日志失败: {}", e.getMessage(), e);
        }
    }
}
