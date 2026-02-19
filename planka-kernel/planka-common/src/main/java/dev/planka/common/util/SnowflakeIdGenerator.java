package dev.planka.common.util;

/**
 * 雪花算法 ID 生成器
 * <p>
 * 基于 Twitter Snowflake 算法实现的分布式唯一 ID 生成器。
 * 生成的 ID 为 64 位 long 类型，结构如下：
 * <ul>
 *     <li>1 位符号位（固定为 0）</li>
 *     <li>41 位时间戳（毫秒级，可使用约 69 年）</li>
 *     <li>10 位工作机器 ID（最多支持 1024 台机器）</li>
 *     <li>12 位序列号（每毫秒最多生成 4096 个 ID）</li>
 * </ul>
 */
public class SnowflakeIdGenerator {

    /** 开始时间戳 (2024-01-01 00:00:00) */
    private static final long EPOCH = 1704067200000L;

    /** 机器 ID 位数 */
    private static final long WORKER_ID_BITS = 10L;

    /** 序列号位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 机器 ID 最大值 (1023) */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 时间戳左移位数 */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 机器 ID 左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 序列号掩码 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 工作机器 ID */
    private final long workerId;

    /** 序列号 */
    private long sequence = 0L;

    /** 上次生成 ID 的时间戳 */
    private long lastTimestamp = -1L;

    /** 默认单例实例 */
    private static volatile SnowflakeIdGenerator instance;

    /**
     * 构造函数
     *
     * @param workerId 工作机器 ID（0-1023）
     */
    public SnowflakeIdGenerator(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker ID 必须在 0 到 " + MAX_WORKER_ID + " 之间");
        }
        this.workerId = workerId;
    }

    /**
     * 获取默认实例
     * <p>
     * 使用默认 workerId = 1
     */
    public static SnowflakeIdGenerator getInstance() {
        if (instance == null) {
            synchronized (SnowflakeIdGenerator.class) {
                if (instance == null) {
                    instance = new SnowflakeIdGenerator(1);
                }
            }
        }
        return instance;
    }

    /**
     * 生成下一个 ID
     *
     * @return 唯一 ID
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 如果当前时间小于上次生成 ID 的时间戳，说明时钟回拨
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成 ID。上次时间戳: " + lastTimestamp + ", 当前时间戳: " + timestamp);
        }

        // 如果是同一毫秒内生成
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 序列号溢出，等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组装 ID
        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成下一个 ID 的字符串形式
     *
     * @return 唯一 ID 字符串
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 静态方法：生成下一个 ID
     */
    public static long generate() {
        return getInstance().nextId();
    }

    /**
     * 静态方法：生成下一个 ID 的字符串形式
     */
    public static String generateStr() {
        return getInstance().nextIdStr();
    }

    /**
     * 等待下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
