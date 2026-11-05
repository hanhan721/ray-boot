package com.ray.schedule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.schedule.domain.JobPageQuery;
import com.ray.schedule.entity.SysJob;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 任务配置写操作提交后异步对齐本机调度器；成功响应表示配置已保存。
 * 暂停或删除不会中断已开始执行的任务。
 */
public interface JobService extends IService<SysJob> {

    IPage<SysJob> page(JobPageQuery query);

    void create(SysJob job);

    void update(SysJob job);

    void delete(Set<Long> ids);

    void pause(Long id);

    void resume(Long id);

    void trigger(Long id);

    CronValidateResult validateCron(String expression);

    record CronValidateResult(boolean valid, List<LocalDateTime> nextTimes) {
    }
}
