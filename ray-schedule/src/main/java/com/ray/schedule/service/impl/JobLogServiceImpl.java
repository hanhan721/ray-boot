package com.ray.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.mybatis.util.PageUtils;
import com.ray.common.mybatis.util.QueryWrapperHelper;
import com.ray.schedule.domain.JobLogPageQuery;
import com.ray.schedule.entity.SysJobLog;
import com.ray.schedule.mapper.JobLogMapper;
import com.ray.schedule.service.JobLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobLogServiceImpl extends ServiceImpl<JobLogMapper, SysJobLog> implements JobLogService {

    @Override
    public IPage<SysJobLog> page(JobLogPageQuery query) {
        return super.page(PageUtils.buildPage(query), QueryWrapperHelper.build(query));
    }

    @Override
    public void clean() {
        baseMapper.truncate();
    }

    @Override
    public long cleanBefore(int retainDays) {
        var cutoff = LocalDateTime.now().minusDays(retainDays);
        var wrapper = new LambdaQueryWrapper<SysJobLog>()
                .lt(SysJobLog::getStartTime, cutoff);
        return baseMapper.delete(wrapper);
    }
}
