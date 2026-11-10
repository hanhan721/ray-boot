package com.ray.schedule.core;

import com.ray.common.core.annotation.ScheduleTask;
import com.ray.schedule.config.ScheduleProperties;
import com.ray.schedule.entity.SysJob;
import com.ray.schedule.entity.SysJobLog;
import com.ray.schedule.event.JobLogEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Spring 原生动态调度管理器
 * <p>
 * 使用 ThreadPoolTaskScheduler + CronTrigger 替代 Quartz，
 * 提供任务的注册、重调度、取消、立即执行能力。
 */
@Slf4j
@Component
public class SpringScheduleManager implements SmartLifecycle {

    private static final int MAX_MSG_LENGTH = 2000;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final ApplicationContext applicationContext;
    private final ScheduleProperties properties;

    /** 已调度的任务 Future */
    private final Map<Long, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    /** 仅记录在途的非并发任务，取消或重排 Future 时不释放执行资格。 */
    private final Set<Long> runningJobs = ConcurrentHashMap.newKeySet();
    private boolean stopping;

    public SpringScheduleManager(ApplicationContext applicationContext,
                                 ScheduleProperties properties,
                                 DataSource dataSource,
                                 RedisConnectionFactory redisConnectionFactory) {
        this.applicationContext = applicationContext;
        this.properties = properties;
        // 配合生命周期停止，保证启动失败时的销毁回调也先于数据库和 Valkey。
        Objects.requireNonNull(dataSource);
        Objects.requireNonNull(redisConnectionFactory);
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(10);
        this.taskScheduler.setThreadNamePrefix("schedule-");
        this.taskScheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.taskScheduler.initialize();
    }

    /**
     * 注册并启动定时任务
     */
    public synchronized void schedule(SysJob job) {
        this.checkRunning();
        this.validateWhitelist(job.getBeanName());
        var trigger = new CronTrigger(job.getCronExpression());
        this.cancel(job.getId());
        Runnable task = this.createTaskRunnable(job);
        var future = taskScheduler.schedule(task, trigger);
        if (future != null) {
            scheduledFutures.put(job.getId(), future);
        }
    }

    /**
     * 重新调度任务（配置变更后调用）
     */
    public void reschedule(SysJob job) {
        this.schedule(job);
    }

    /**
     * 取消任务（删除或禁用时调用）
     */
    public synchronized void cancel(Long jobId) {
        var future = scheduledFutures.remove(jobId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 立即触发一次（不影响正常调度）
     */
    public synchronized void triggerNow(SysJob job) {
        this.checkRunning();
        this.validateWhitelist(job.getBeanName());
        taskScheduler.execute(this.createTaskRunnable(job));
    }

    /**
     * 取消所有任务（全量重建前调用）
     */
    public synchronized void cancelAll() {
        scheduledFutures.forEach((id, future) -> future.cancel(false));
        scheduledFutures.clear();
    }

    @Override
    public void start() {
        // 线程池在构造时初始化；本组件不参与暂停后的重启。
    }

    @Override
    public boolean isPauseable() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return !taskScheduler.getScheduledExecutor().isTerminated();
    }

    @Override
    public int getPhase() {
        // HTTP 优雅停机之后、框架调度池和 Valkey 连接工厂停止之前收尾。
        return ThreadPoolTaskScheduler.DEFAULT_PHASE + 1;
    }

    @Override
    public void stop() {
        this.shutdown();
    }

    /**
     * 拒绝新任务，取消尚未开始的触发，并等待在途任务结束。
     * 优雅等待超时后中断执行线程，按配置间隔告警并继续等待实际退出。
     * 同步阻塞生命周期停止，避免阶段超时后提前关闭任务仍需使用的资源；
     * 销毁回调仅作为启动失败等未经过生命周期停止场景的兜底。
     */
    @PreDestroy
    public void shutdown() {
        List<ScheduledFuture<?>> futures;
        synchronized (this) {
            stopping = true;
            futures = List.copyOf(scheduledFutures.values());
        }
        futures.forEach(future -> future.cancel(false));
        var executor = taskScheduler.getScheduledExecutor();
        executor.shutdown();
        var graceSeconds = properties.shutdownGraceSeconds();
        var interruptSeconds = properties.shutdownInterruptSeconds();
        boolean interrupted = false;
        try {
            try {
                if (executor.awaitTermination(graceSeconds, TimeUnit.SECONDS)) {
                    return;
                }
                log.warn("调度任务未在 {} 秒内结束，中断执行线程", graceSeconds);
            } catch (InterruptedException e) {
                interrupted = true;
                log.warn("等待调度任务结束时被中断，中断任务后继续等待收尾");
            }
            executor.shutdownNow();
            while (!executor.isTerminated()) {
                try {
                    if (!executor.awaitTermination(interruptSeconds, TimeUnit.SECONDS)) {
                        this.logSurvivingThreads();
                    }
                } catch (InterruptedException e) {
                    // 先等任务真正退出，最后再恢复关闭线程的中断标记。
                    interrupted = true;
                }
            }
        } finally {
            if (executor.isTerminated()) {
                scheduledFutures.clear();
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void logSurvivingThreads() {
        var names = Thread.getAllStackTraces().keySet().stream()
                .map(Thread::getName)
                .filter(name -> name.startsWith("schedule-") && !name.startsWith("schedule-sync-"))
                .toList();
        log.error("调度线程在中断后仍未退出，继续等待并保留依赖资源: {}", names);
    }

    private void checkRunning() {
        if (stopping) {
            throw new RejectedExecutionException("调度器正在关闭，拒绝注册或触发任务");
        }
    }

    /**
     * 创建任务执行 Runnable
     * <p>
     * 将 SysJob 的配置快照捕获到闭包中，包含：
     * - 并发控制（concurrent=false 时跳过重叠执行）
     * - 反射调用目标 Bean 方法
     * - 执行日志记录与事件发布
     */
    private Runnable createTaskRunnable(SysJob job) {
        Long jobId = job.getId();
        boolean concurrent = Boolean.TRUE.equals(job.getConcurrent());
        String beanName = job.getBeanName();
        String methodName = job.getMethodName();
        String methodParams = job.getMethodParams();
        String jobName = job.getJobName();
        String jobGroup = job.getJobGroup();

        return () -> {
            // 并发控制：非并发任务在前一次未完成时跳过
            if (!concurrent && !runningJobs.add(jobId)) {
                log.debug("任务 [{}] 禁止并发执行，跳过本次触发", jobName);
                return;
            }

            var jobLog = new SysJobLog()
                    .setJobId(jobId)
                    .setJobName(jobName)
                    .setJobGroup(jobGroup)
                    .setBeanName(beanName)
                    .setMethodName(methodName)
                    .setMethodParams(methodParams);

            var startTime = LocalDateTime.now();
            long beginMs = System.currentTimeMillis();

            try {
                Object bean = applicationContext.getBean(beanName);
                Object result = invokeMethod(bean, methodName, methodParams);
                jobLog.setSuccess(true)
                        .setResult(truncate(result != null ? result.toString() : null));
            } catch (Exception e) {
                Throwable cause = e instanceof InvocationTargetException ite
                        ? ite.getTargetException() : e;
                jobLog.setSuccess(false)
                        .setErrorMsg(truncate(cause.getMessage()));
                log.error("任务执行失败: {}.{}", beanName, methodName, e);
            } finally {
                jobLog.setStartTime(startTime)
                        .setEndTime(LocalDateTime.now())
                        .setCostTime((int) (System.currentTimeMillis() - beginMs));
                try {
                    applicationContext.publishEvent(new JobLogEvent(jobLog));
                } catch (RuntimeException e) {
                    // 停机期间事件分发可能失败，不能阻碍任务线程收尾和并发资格释放。
                    log.warn("任务 [{}] 执行日志事件发布失败", jobName, e);
                } finally {
                    if (!concurrent) {
                        runningJobs.remove(jobId);
                    }
                }
            }
        };
    }

    private static Object invokeMethod(Object bean, String methodName, String methodParams)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (StringUtils.hasText(methodParams)) {
            Method method = bean.getClass().getMethod(methodName, String.class);
            return method.invoke(bean, methodParams);
        }
        Method method = bean.getClass().getMethod(methodName);
        return method.invoke(bean);
    }

    private static String truncate(String str) {
        if (str == null || str.length() <= MAX_MSG_LENGTH) {
            return str;
        }
        return str.substring(0, MAX_MSG_LENGTH);
    }

    /**
     * 校验目标 Bean 是否标注了 @ScheduleTask 白名单注解
     */
    private void validateWhitelist(String beanName) {
        Object bean = applicationContext.getBean(beanName);
        if (!bean.getClass().isAnnotationPresent(ScheduleTask.class)) {
            throw new IllegalStateException(
                    "Bean [" + beanName + "] 未标注 @ScheduleTask，拒绝调度执行");
        }
    }
}
