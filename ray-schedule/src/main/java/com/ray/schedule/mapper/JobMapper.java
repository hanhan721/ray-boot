package com.ray.schedule.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ray.schedule.entity.SysJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务 Mapper
 */
@Mapper
public interface JobMapper extends MPJBaseMapper<SysJob> {
}
