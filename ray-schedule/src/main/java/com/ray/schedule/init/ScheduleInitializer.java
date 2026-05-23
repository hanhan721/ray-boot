package com.ray.schedule.init;

import com.ray.schedule.core.SpringScheduleManager;
import com.ray.schedule.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时从 sys_job 全量加载定时任务到内存调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleInitializer implements ApplicationRunner {

    private final SpringScheduleManager scheduleManager;
    private final JobMapper jobMapper;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        var jobs = jobMapper.selectList(null);

        int registered = 0, skipped = 0, failed = 0;

        for (var job : jobs) {
            if (!Boolean.TRUE.equals(job.getEnabled())) {
                skipped++;
                continue;
            }
            try {
                scheduleManager.schedule(job);
                registered++;
            } catch (Exception e) {
                failed++;
                log.error("注册定时任务失败: {} - {}", job.getJobName(), e.getMessage(), e);
            }
        }

        log.info("定时任务加载完成: 注册 {} 个, 跳过(禁用) {} 个, 失败 {} 个",
                registered, skipped, failed);
    }
}
