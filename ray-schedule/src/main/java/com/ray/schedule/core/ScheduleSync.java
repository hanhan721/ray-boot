package com.ray.schedule.core;

import com.ray.schedule.mapper.JobMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 单实例调度对齐入口：提交回调只投递 ID，专用线程串行完成读库和调度变更。
 * <p>
 * 成功响应表示配置已提交，内存调度随后生效。队列和对齐失败仅记录日志，
 * 可通过再次修改、暂停或恢复任务重试；已删除任务须重发删除请求或重启实例。
 * 不做周期对账，也不提供跨实例同步。
 */
@Slf4j
@Component
public class ScheduleSync {

    private static final int QUEUE_CAPACITY = 1024;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final SpringScheduleManager scheduleManager;
    private final JobMapper jobMapper;
    private final ThreadPoolExecutor worker;

    public ScheduleSync(SpringScheduleManager scheduleManager, JobMapper jobMapper) {
        this.scheduleManager = scheduleManager;
        this.jobMapper = jobMapper;
        this.worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                Thread.ofPlatform().name("schedule-sync-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** 在业务事务内注册；回滚时不触碰原有 Future。 */
    public void alignAfterCommit(Collection<Long> jobIds) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("调度对齐必须在数据库事务内注册");
        }
        var ids = List.copyOf(jobIds);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                // 不在提交线程读库，也不等待工作器，避免复用或等待原事务持有的连接。
                // 完成回调逐个隔离异常，其他 afterCommit 回调失败也不会跳过本通知。
                if (status == STATUS_COMMITTED) {
                    ScheduleSync.this.submit(ids);
                }
            }
        });
    }

    /** 启动恢复也只传 ID，查询快照不会直接应用到调度器。 */
    public void alignOnStartup(Collection<Long> jobIds) {
        this.submit(List.copyOf(jobIds));
    }

    private void submit(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        try {
            worker.execute(() -> {
                for (var id : ids) {
                    if (Thread.currentThread().isInterrupted()) {
                        log.warn("调度对齐工作器已中断，当前批次的剩余通知将在重启后按库恢复");
                        return;
                    }
                    this.align(id);
                }
            });
        } catch (RejectedExecutionException e) {
            // 禁止 CallerRuns 或静默丢弃，确保所有读库和调度变更始终串行执行。
            log.error("任务 {} 调度对齐入队失败；请重试任务操作（已删除任务重发删除请求）或重启实例", ids, e);
        }
    }

    private void align(Long jobId) {
        try {
            var job = jobMapper.selectById(jobId);
            if (Thread.currentThread().isInterrupted()) {
                log.warn("任务 [{}] 对齐期间工作器被中断，将在重启后按库恢复", jobId);
                return;
            }
            if (job == null || !Boolean.TRUE.equals(job.getEnabled())) {
                scheduleManager.cancel(jobId);
            } else {
                scheduleManager.schedule(job);
            }
        } catch (RuntimeException e) {
            log.error("任务 [{}] 调度对齐失败；请重试任务操作（已删除任务重发删除请求）或重启实例", jobId, e);
        }
    }

    /** 依赖关系保证本工作器先于调度器和数据源销毁，尽量处理完已提交的通知。 */
    @PreDestroy
    public void shutdown() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                var discarded = worker.shutdownNow();
                log.warn("调度对齐工作器停止超时，丢弃 {} 批通知；重启后将重新按库加载", discarded.size());
            }
        } catch (InterruptedException e) {
            worker.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
