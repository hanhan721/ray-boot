package com.ray.system.file;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.function.Supplier;

/** 只协调元数据，锁内禁止任何存储 I/O。 */
@Component
public class FileTransactions {
    private final TransactionTemplate transaction;
    private final JdbcTemplate jdbc;

    public FileTransactions(PlatformTransactionManager manager, JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(15);
    }

    /**
     * 在独立短事务中执行元数据操作，限制锁等待与 SQL 执行时间。
     * 回调不得访问存储，完成后即提交并释放数据库连接及事务级 advisory lock。
     */
    public <T> T execute(Supplier<T> work) {
        return transaction.execute(_ -> {
            // 1.只对当前事务设置超时，避免污染连接池中后续复用该连接的请求。
            jdbc.execute("SET LOCAL lock_timeout = '5s'");
            jdbc.execute("SET LOCAL statement_timeout = '15s'");
            // 2.执行发布或 GC 状态变更；异常由事务模板统一回滚。
            return work.get();
        });
    }

    /** 在当前事务内协调同一存储命名空间、同一内容的发布与回收状态变更。 */
    public void lock(String storageId, String hash) {
        this.lockAll(java.util.List.of(lockId(storageId, hash)));
    }

    /** 先按实际 64 位锁号排序并去重，再顺序加锁，减少跨实例多锁死锁风险。 */
    public void lockAll(Collection<Long> ids) {
        ids.stream().distinct().sorted().forEach(id ->
                jdbc.queryForList("SELECT pg_advisory_xact_lock(?)", id));
    }

    /**
     * 通过固定 UTF-8 输入和 SHA-256 前 64 位生成跨实例一致的锁号。
     * 前缀与编码规则属于协调协议，变更时必须统一迁移所有运行实例。
     */
    public static long lockId(String storageId, String hash) {
        try {
            var bytes = MessageDigest.getInstance("SHA-256")
                    .digest(("ray:file:v1\n" + storageId + "\n" + hash).getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(bytes).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
