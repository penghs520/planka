package dev.planka.card.service.sequence;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 号段
 * 表示一段连续的编号范围 [start, end]
 */
public class Segment {

    /**
     * 号段起始值（包含）
     */
    private final long start;

    /**
     * 号段结束值（包含）
     */
    private final long end;

    /**
     * 当前值（下一个要分配的值）
     */
    private final AtomicLong current;

    public Segment(long start, long end) {
        this.start = start;
        this.end = end;
        this.current = new AtomicLong(start);
    }

    /**
     * 获取下一个编号
     *
     * @return 下一个编号，如果号段已耗尽返回 -1
     */
    public long getNext() {
        long value = current.getAndIncrement();
        if (value > end) {
            return -1; // 号段已耗尽
        }
        return value;
    }

    /**
     * 判断号段是否已耗尽
     */
    public boolean isExhausted() {
        return current.get() > end;
    }

    /**
     * 获取使用率
     *
     * @return 使用率，0.0 到 1.0
     */
    public double getUsageRatio() {
        long used = current.get() - start;
        long total = end - start + 1;
        return (double) used / total;
    }

    /**
     * 获取剩余数量
     */
    public long getRemaining() {
        return Math.max(0, end - current.get() + 1);
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }
}
