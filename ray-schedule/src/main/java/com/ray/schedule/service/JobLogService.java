package com.ray.schedule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.schedule.domain.JobLogPageQuery;
import com.ray.schedule.entity.SysJobLog;

public interface JobLogService extends IService<SysJobLog> {

    IPage<SysJobLog> page(JobLogPageQuery query);

    void clean();

    /**
     * 清理指定天数前的日志
     *
     * @param retainDays 保留最近多少天
     * @return 删除的记录数
     */
    long cleanBefore(int retainDays);
}
