package com.ray.system.file;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ray.common.storage.storage.StorageService;
import com.ray.system.api.entity.SysFileObject;
import com.ray.system.mapper.FileMapper;
import com.ray.system.mapper.FileObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.function.BooleanSupplier;

/** 按主键游标分批执行文件清理，元数据状态变更与事务外字节删除分开进行。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileGarbageCollector {
    private final FileMapper files;
    private final FileObjectMapper objects;
    private final FileTransactions transactions;
    private final FileBindings bindings;
    private final StorageService storage;

    /**
     * 使用本轮固定保留期执行清理；每批最多 100 项，失去任务租约后停止领取新批次。
     * 每个文件或对象使用独立短事务，单项失败记入报告并留待后续轮次重试。
     */
    public Report run(FileRetention.Policy policy, boolean scanOrphans, BooleanSupplier leaseOwned) {
        // 1.拒绝外层事务或同步上下文，保证后续存储删除不占用数据库连接。
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
                || org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("文件清理必须在事务及事务同步上下文外调用");
        }
        var report = new Report();
        var cutoff = LocalDateTime.now().minusHours(policy.unboundHours());
        // 2.按逻辑文件主键扫描过期未绑定记录，逐项在行锁内复查并逻辑删除。
        long after = 0;
        while (leaseOwned.getAsBoolean()) {
            var batch = files.expired(after, cutoff);
            if (batch.isEmpty()) break;
            for (long id : batch) {
                try {
                    if (transactions.execute(() -> bindings.expire(id, cutoff))) report.expired++;
                } catch (RuntimeException e) { this.failed(report, "未绑定文件/业务引用异常", id, e); }
            }
            after = batch.getLast();
        }
        // 3.按物理对象主键扫描，观察零引用并声明回收，提交后才删除字节。
        after = 0;
        while (leaseOwned.getAsBoolean()) {
            var batch = objects.scan(after);
            if (batch.isEmpty()) break;
            for (var candidate : batch) {
                try {
                    var deleting = transactions.execute(() -> this.observe(candidate.getId(), policy, report));
                    if (deleting != null) this.purge(deleting, report);
                } catch (RuntimeException e) { this.failed(report, "对象回收失败，后续轮次将重试", candidate.getId(), e); }
            }
            after = batch.getLast().getId();
        }
        // 4.需要孤儿扫描且仍持有租约时，清理超龄且无有效对象元数据的 key。
        if (scanOrphans && leaseOwned.getAsBoolean()) this.orphans(policy, leaseOwned, report);
        return report;
    }

    /**
     * 在短事务及 hash 锁内复查引用并推进状态；返回 DELETING 对象供事务外删除字节。
     * 本方法只访问数据库，不调用存储后端。
     */
    private SysFileObject observe(long id, FileRetention.Policy policy, Report report) {
        // 1.确认候选对象及存储身份，取得与上传发布共用的 hash 锁。
        var candidate = objects.selectById(id);
        if (candidate == null || "PURGED".equals(candidate.getStatus())) return null;
        this.checkIdentity(candidate);
        transactions.lock(candidate.getStorageId(), candidate.getFileHash());
        // 2.锁等待后重新读取状态与活跃引用，不能沿用候选扫描时的快照。
        var object = objects.fresh(id);
        if ("PURGED".equals(object.getStatus())) return null;
        var active = files.activeReference(id);
        // 3.回收中对象继续交给删除阶段；若仍有活跃引用则报告异常并阻断删除。
        if ("DELETING".equals(object.getStatus())) {
            if (active) throw new IllegalStateException("回收中对象仍有活跃引用");
            return object;
        }
        // 4.发现活跃引用时清除零引用计时，防止复用后的对象沿用旧回收期限。
        if (active) {
            if (object.getUnreferencedSince() != null) {
                objects.update(null, this.update(id).set(SysFileObject::getUnreferencedSince, null));
                report.reused++;
            }
            return null;
        }
        // 5.首次无引用只记录时间；超过保留期才声明 DELETING，不在此处删除字节。
        var now = LocalDateTime.now();
        if (object.getUnreferencedSince() == null) {
            objects.update(null, this.update(id).set(SysFileObject::getUnreferencedSince, now));
            report.observed++;
        } else if (!object.getUnreferencedSince().plusDays(policy.objectDays()).isAfter(now)) {
            objects.update(null, this.update(id).set(SysFileObject::getStatus, "DELETING")
                    .set(SysFileObject::getDeleteStartedAt, now));
            object.setStatus("DELETING");
            report.declared++;
            return object;
        }
        return null;
    }

    /**
     * 删除已声明回收对象的字节，再用短事务确认 PURGED。
     * 删除失败保留 DELETING；删除成功但确认中断时，下轮可按字节不存在继续完成回收。
     */
    private void purge(SysFileObject object, Report report) {
        // 1.事务、连接和 hash 锁均已释放，迟到删除只触及此对象独有的历史 key。
        if (!storage.exists(object.getObjectKey())) {
            log.warn("文件字节缺失: objectId={}, key={}", object.getId(), object.getObjectKey());
            report.missing++;
        }
        storage.delete(object.getObjectKey());
        // 2.存储确认成功后重新取得 hash 锁，复核状态和引用，再标记 PURGED。
        boolean changed = transactions.execute(() -> {
            transactions.lock(object.getStorageId(), object.getFileHash());
            var current = objects.selectById(object.getId());
            if (!"DELETING".equals(current.getStatus())) return false;
            if (files.activeReference(object.getId())) throw new IllegalStateException("回收中对象出现活跃引用");
            objects.update(null, this.update(object.getId()).set(SysFileObject::getStatus, "PURGED")
                    .set(SysFileObject::getPurgedAt, LocalDateTime.now()));
            return true;
        });
        // 3.仅在本次完成状态迁移时累计回收数量和字节，避免重复确认重复计数。
        if (changed) {
            report.purged++;
            report.bytes += object.getFileSize();
        }
    }

    /** 分页清理超龄孤儿 key，包括 LOCAL 崩溃遗留临时文件；删除前再次确认数据库未跟踪。 */
    private void orphans(FileRetention.Policy policy, BooleanSupplier leaseOwned, Report report) {
        // 1.固定本轮年龄阈值，并在领取每个存储分页前检查租约。
        var before = Instant.now().minus(policy.orphanRetention());
        String cursor = null;
        do {
            if (!leaseOwned.getAsBoolean()) return;
            // 2.流式取得至多 100 个候选，只处理修改时间早于阈值的对象。
            var page = storage.list(cursor);
            for (var item : page.objects()) {
                if (!item.modifiedAt().isBefore(before)) continue;
                try {
                    // 3.过滤仍有 AVAILABLE/DELETING 元数据的 key，删除前再查一次。
                    if (objects.tracked(storage.getStorageId(), item.key())) continue;
                    // 年龄阈值用于避开正常上传的元数据提交窗口。
                    // 假定上传流程不会暂停超过孤儿保留期后继续提交。
                    // 极端情况下，跨越该期限恢复的上传可能引用已被清理的字节；
                    // 本系统接受这一风险，不增加 key 锁或上传登记机制。
                    if (objects.tracked(storage.getStorageId(), item.key())) continue;
                    // 4.逐项确认删除成功后累计统计，失败只影响当前 key。
                    storage.delete(item.key());
                    report.orphans++;
                    report.bytes += item.size();
                } catch (RuntimeException e) {
                    this.failed(report, "孤儿清理失败: " + item.key(), 0, e);
                }
            }
            cursor = page.nextCursor();
        } while (cursor != null);
    }

    /** 回收也必须核对存储身份，避免错误配置将历史 key 指向其他后端。 */
    private void checkIdentity(SysFileObject object) {
        if (!storage.getStorageId().equals(object.getStorageId()) || storage.getStorageType() != object.getStorageType()
                || !storage.getBucket().equals(object.getBucket())) throw new IllegalStateException("文件存储身份不一致");
    }

    private LambdaUpdateWrapper<SysFileObject> update(long id) {
        return new LambdaUpdateWrapper<SysFileObject>().eq(SysFileObject::getId, id);
    }

    /** 汇总单项失败并保留阶段、对象编号及原因，供持续回收失败排查。 */
    private void failed(Report report, String stage, long id, RuntimeException e) {
        report.failures++;
        log.error("{}: id={}, reason={}", stage, id, e.getMessage(), e);
    }

    /** 本轮各阶段的处理计数、回收字节及异常计数，供调度日志展示。 */
    public static final class Report {
        public long expired, observed, reused, declared, purged, orphans, bytes, missing, failures;
        @Override
        public String toString() {
            return "expired=" + expired + ", observed=" + observed + ", reused=" + reused + ", declared=" + declared
                    + ", purged=" + purged + ", orphans=" + orphans + ", bytes=" + bytes
                    + ", missing=" + missing + ", failures=" + failures;
        }
    }
}
