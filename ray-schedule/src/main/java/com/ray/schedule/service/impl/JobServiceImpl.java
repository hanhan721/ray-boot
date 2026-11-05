package com.ray.schedule.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.core.util.AssertUtils;
import com.ray.common.mybatis.util.PageUtils;
import com.ray.common.mybatis.util.QueryWrapperHelper;
import com.ray.schedule.core.ScheduleSync;
import com.ray.schedule.core.SpringScheduleManager;
import com.ray.common.core.annotation.ScheduleTask;
import com.ray.schedule.domain.JobPageQuery;
import com.ray.schedule.entity.SysJob;
import com.ray.schedule.mapper.JobMapper;
import com.ray.schedule.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl extends ServiceImpl<JobMapper, SysJob> implements JobService {

    private final SpringScheduleManager scheduleManager;
    private final ScheduleSync scheduleSync;
    private final ApplicationContext applicationContext;

    @Override
    public IPage<SysJob> page(JobPageQuery query) {
        return super.page(PageUtils.buildPage(query), QueryWrapperHelper.build(query));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysJob job) {
        this.validateBeanAndMethod(job);
        this.validateCronExpression(job.getCronExpression());
        super.save(job);
        scheduleSync.alignAfterCommit(List.of(job.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysJob job) {
        AssertUtils.notNull(job.getId(), "任务 ID 不能为空");
        var existing = super.getById(job.getId());
        AssertUtils.notNull(existing, "任务不存在");
        this.validateBeanAndMethod(job);
        this.validateCronExpression(job.getCronExpression());
        super.updateById(job);
        scheduleSync.alignAfterCommit(List.of(job.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        super.removeByIds(ids);
        scheduleSync.alignAfterCommit(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pause(Long id) {
        var job = super.getById(id);
        AssertUtils.notNull(job, "任务不存在");
        job.setEnabled(false);
        super.updateById(job);
        scheduleSync.alignAfterCommit(List.of(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resume(Long id) {
        var job = super.getById(id);
        AssertUtils.notNull(job, "任务不存在");
        job.setEnabled(true);
        super.updateById(job);
        scheduleSync.alignAfterCommit(List.of(id));
    }

    @Override
    public void trigger(Long id) {
        var job = super.getById(id);
        AssertUtils.notNull(job, "任务不存在");
        scheduleManager.triggerNow(job);
    }

    @Override
    public CronValidateResult validateCron(String expression) {
        try {
            var cron = CronExpression.parse(expression);
            var nextTimes = new ArrayList<LocalDateTime>(5);
            var next = LocalDateTime.now();
            for (int i = 0; i < 5; i++) {
                next = cron.next(next);
                if (next == null) {
                    break;
                }
                nextTimes.add(next);
            }
            return new CronValidateResult(true, nextTimes);
        } catch (IllegalArgumentException e) {
            return new CronValidateResult(false, List.of());
        }
    }

    private void validateBeanAndMethod(SysJob job) {
        Object bean;
        try {
            bean = applicationContext.getBean(job.getBeanName());
        } catch (Exception e) {
            throw new BusinessException("Spring Bean [" + job.getBeanName() + "] 不存在");
        }
        if (!bean.getClass().isAnnotationPresent(ScheduleTask.class)) {
            throw new BusinessException("Bean [" + job.getBeanName() + "] 未标注 @ScheduleTask，不允许作为调度任务");
        }
        try {
            if (StringUtils.hasText(job.getMethodParams())) {
                bean.getClass().getMethod(job.getMethodName(), String.class);
            } else {
                bean.getClass().getMethod(job.getMethodName());
            }
        } catch (NoSuchMethodException e) {
            throw new BusinessException("方法 [" + job.getMethodName() + "] 在 Bean ["
                    + job.getBeanName() + "] 中不存在");
        }
    }

    private void validateCronExpression(String expression) {
        try {
            CronExpression.parse(expression);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Cron 表达式 [" + expression + "] 不合法");
        }
    }
}
