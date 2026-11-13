package com.ray.common.core.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 53 位雪花 ID 生成器
 * <p>
 * 位分配：40(时间) + 3(机器) + 10(序列) = 53 位
 * <ul>
 *   <li>时间单位：毫秒（相对自定义 epoch）</li>
 *   <li>机器 ID：3 位（0..7）</li>
 *   <li>序列号：10 位（每毫秒每机 0..1023）</li>
 * </ul>
 * <p>
 * 设计目标：
 * <ul>
 *   <li>≤ 53 位：JS Number 不丢精度</li>
 *   <li>CAS 无锁：高并发下开销更小</li>
 *   <li>单调递增（同机同毫秒内按序列增长；跨毫秒按时间增长）</li>
 * </ul>
 */
public final class Snowflake53 {

    /**
     * 基准时间戳（2025-01-01 00:00:00 UTC）
     * <p>
     * 选择近年的时间避免高位逼近 2^53
     */
    private static final long EPOCH_MS = 1735689600000L;

    // ===== 位宽配置（40 + 3 + 10 = 53） =====
    private static final int TIME_BITS = 40;     // 约 34.8 年可用期
    private static final int MACHINE_BITS = 3;   // 8 台
    private static final int SEQ_BITS = 10;      // 每毫秒 1024 个/机

    // ===== 移位与掩码 =====
    private static final int SEQ_SHIFT = 0;
    private static final int MACHINE_SHIFT = SEQ_BITS;                // 10
    private static final int TIME_SHIFT = MACHINE_BITS + SEQ_BITS;    // 13

    private static final long SEQ_MASK = (1L << SEQ_BITS) - 1;            // 0x3FF
    private static final long MAX_MACHINE_ID = (1L << MACHINE_BITS) - 1;  // 0x7
    private static final long MAX_DELTA_MS = (1L << TIME_BITS) - 1;       // 2^40 - 1
    private static final long JS_SAFE_MAX = (1L << 53) - 1;               // 2^53 - 1

    /**
     * state 高位是"最近毫秒 deltaMs"，低位是"最近序列号 seq"
     * <p>
     * layout: [ deltaMs (40 bits) | seq (10 bits) ]
     * <p>
     * 注意：这里的 state 不包含 machineId（machineId 在最终拼装返回值时再并入）
     */
    private final AtomicLong state = new AtomicLong(-1L);

    /**
     * 机器 ID：0..7
     */
    private final long machineId;

    public Snowflake53(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException("机器ID必须在 [0, " + MAX_MACHINE_ID + "] 范围内");
        }
        this.machineId = machineId;
    }

    /**
     * 生成下一个 ID（long，保证 < 2^53）
     */
    public long nextId() {
        while (true) {
            long nowMs = System.currentTimeMillis();
            long delta = nowMs - EPOCH_MS;

            long prev = state.get();
            long prevDelta = prev < 0 ? -1 : (prev >>> SEQ_BITS); // 取出上次毫秒
            long prevSeq = prev & SEQ_MASK;                       // 取出上次序列

            // 时钟回拨：若 delta < prevDelta，则等待到 prevDelta+1 对应的真实时间
            if (prev >= 0 && delta < prevDelta) {
                long targetMs = (prevDelta + 1) + EPOCH_MS;
                waitUntil(targetMs);
                continue; // 重新读取 state 与时间
            }

            if (delta == prevDelta) {
                // 同一毫秒：尝试递增序列
                if (prevSeq == SEQ_MASK) {
                    // 序列用尽 -> 等下一毫秒
                    waitUntil(nowMs + 1);
                    continue;
                }
                long newSeq = prevSeq + 1;
                long newState = (delta << SEQ_BITS) | newSeq;
                if (state.compareAndSet(prev, newState)) {
                    return packId(delta, newSeq);
                }
            } else {
                // 跨毫秒（或首次）：序列归零
                if (delta < 0) {
                    throw new IllegalStateException("系统时间早于基准时间(EPOCH_MS)");
                }
                if (delta > MAX_DELTA_MS) {
                    throw new IllegalStateException("时间位已耗尽(TIME_BITS=" + TIME_BITS + ")");
                }
                long newState = (delta << SEQ_BITS); // seq=0
                if (state.compareAndSet(prev, newState)) {
                    return packId(delta, 0L);
                }
            }
        }
    }

    /**
     * 生成字符串形式
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 获取机器 ID
     */
    public long getMachineId() {
        return machineId;
    }

    /**
     * 获取最大机器 ID
     */
    public static long getMaxMachineId() {
        return MAX_MACHINE_ID;
    }

    // ===== 内部工具 =====

    private static void waitUntil(long targetMs) {
        long remainingMs;
        while ((remainingMs = targetMs - System.currentTimeMillis()) > 0) {
            try {
                Thread.sleep(Math.min(remainingMs, 10L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待生成雪花ID时线程被中断", e);
            }
        }
    }

    /**
     * 组装最终 ID：[ deltaMs | machineId | seq ]
     */
    private long packId(long deltaMs, long seq) {
        long id = (deltaMs << TIME_SHIFT)
                | (machineId << MACHINE_SHIFT)
                | (seq << SEQ_SHIFT);

        // 保险线：确保不超过 53 位安全整数
        if (id < 0 || id > JS_SAFE_MAX) {
            throw new IllegalStateException("ID超过53位安全整数范围: " + id);
        }
        return id;
    }

    // ===== 调试/解析工具 =====

    /**
     * 解析 ID，便于自检与排障
     *
     * @param id 雪花 ID
     * @return 解析后的组件
     */
    public static Components unpack(long id) {
        long seq = id & SEQ_MASK;
        long machineId = (id >>> MACHINE_SHIFT) & MAX_MACHINE_ID;
        long deltaMs = id >>> TIME_SHIFT;
        long tsMs = EPOCH_MS + deltaMs;
        return new Components(tsMs, (int) machineId, (int) seq);
    }

    /**
     * ID 解析结果
     *
     * @param timestampMs 生成时间戳（毫秒）
     * @param machineId   机器 ID
     * @param sequence    序列号
     */
    public record Components(long timestampMs, int machineId, int sequence) {
    }
}
