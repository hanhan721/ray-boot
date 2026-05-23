package com.ray.schedule.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ray.schedule.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 任务执行日志 Mapper
 */
@Mapper
public interface JobLogMapper extends MPJBaseMapper<SysJobLog> {

    @Update("TRUNCATE TABLE sys_job_log")
    void truncate();
}
