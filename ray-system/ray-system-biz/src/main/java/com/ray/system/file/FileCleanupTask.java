package com.ray.system.file;

import com.ray.common.core.annotation.ScheduleTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** 文件清理调度入口；Valkey 租约减少重复劳动，正确性由逐项数据库校验保证。 */
@Slf4j
@Component("fileCleanupTask")
@ScheduleTask
@RequiredArgsConstructor
public class FileCleanupTask {
    private static final String LOCK = "ray:file:cleanup:lease";
    private static final DefaultRedisScript<Long> RENEW = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('pexpire',KEYS[1],300000) else return 0 end", Long.class);
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end", Long.class);
    private final StringRedisTemplate redis;
    private final FileRetention retention;
    private final FileGarbageCollector collector;

    /**
     * 由 sys_job 按应用时区每日 03:00 调度，也可从任务管理手动触发；周日附加孤儿扫描。
     * 使用随机所有者令牌申请五分钟租约，每分钟原子续期，释放时原子核对所有者。
     */
    public String execute() {
        // 1.读取本轮保留期快照，参数非法时直接终止，不进入清理流程。
        var policy = retention.read(); // 格式错误立即终止本轮，不能悄悄回退。
        // 2.申请带随机所有者令牌的租约，已有任务运行时跳过本轮。
        var owner = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(LOCK, owner, Duration.ofMinutes(5)))) return "已有清理任务运行";
        var valid = new AtomicBoolean(true);
        var renewedAt = new java.util.concurrent.atomic.AtomicLong(System.nanoTime());
        var renewal = Executors.newSingleThreadScheduledExecutor(r ->
                Thread.ofPlatform().daemon().name("file-cleanup-lease").unstarted(r));
        try {
            // 3.定期续期；令牌不匹配或续期异常后，永久停止本轮领取新批次。
            renewal.scheduleAtFixedRate(() -> {
                try {
                    if (!Long.valueOf(1).equals(redis.execute(RENEW, List.of(LOCK), owner))) valid.set(false);
                    else renewedAt.set(System.nanoTime());
                } catch (RuntimeException e) {
                    valid.set(false);
                    log.error("文件清理租约续期失败，停止领取新批次", e);
                }
            }, 1, 1, TimeUnit.MINUTES);
            // 4.执行分阶段清理，同时用单调时钟防止长时间暂停后沿用已过期租约。
            var report = collector.run(policy, LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY,
                    () -> valid.get() && System.nanoTime() - renewedAt.get() < Duration.ofMinutes(5).toNanos());
            log.info("文件清理完成: {}", report);
            return report.toString();
        } finally {
            // 5.停止续期，仅释放仍属于本任务的租约，避免误删其他实例新取得的锁。
            valid.set(false);
            renewal.shutdownNow();
            try { redis.execute(RELEASE, List.of(LOCK), owner); }
            catch (RuntimeException e) { log.warn("文件清理租约释放失败，等待到期", e); }
        }
    }
}
