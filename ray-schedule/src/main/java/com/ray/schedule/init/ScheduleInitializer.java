package com.ray.schedule.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.schedule.core.ScheduleSync;
import com.ray.schedule.entity.SysJob;
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

    private final ScheduleSync scheduleSync;
    private final JobMapper jobMapper;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        var jobs = jobMapper.selectList(new LambdaQueryWrapper<SysJob>().select(SysJob::getId));
        scheduleSync.alignOnStartup(jobs.stream().map(SysJob::getId).toList());
        log.info("定时任务启动对齐已提交，共 {} 个任务；执行结果以对齐日志为准", jobs.size());
    }
}
