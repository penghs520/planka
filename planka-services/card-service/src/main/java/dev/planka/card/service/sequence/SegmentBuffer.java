package dev.planka.card.service.sequence;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 号段缓冲区
 * 管理当前正在使用的号段和预加载的下一个号段
 */
public class SegmentBuffer {

    /**
     * 组织ID
     */
    private final String orgId;

    /**
     * 当前正在使用的号段
     */
    private volatile Segment currentSegment;

    /**
     * 预加载的下一个号段
     */
    private volatile Segment nextSegment;

    /**
     * 是否正在加载下一个号段
     */
    private final AtomicBoolean loadingNext = new AtomicBoolean(false);

    /**
     * 下一个号段是否已准备好
     */
    private volatile boolean nextReady = false;

    /**
     * 锁，用于切换号段
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 触发预加载的使用率阈值
     */
    private static final double PRELOAD_THRESHOLD = 0.7;

    public SegmentBuffer(String orgId) {
        this.orgId = orgId;
    }

    /**
     * 获取下一个编号
     *
     * @param segmentLoader 号段加载器，当需要获取新号段时调用
     * @return 下一个编号
     * @throws RuntimeException 如果获取编号失败
     */
    public long getNext(Supplier<Segment> segmentLoader) {
        // 如果当前号段为空，需要先加载
        if (currentSegment == null) {
            lock.lock();
            try {
                if (currentSegment == null) {
                    currentSegment = segmentLoader.get();
                }
            } finally {
                lock.unlock();
            }
        }

        // 尝试从当前号段获取
        long value = currentSegment.getNext();

        if (value != -1) {
            // 检查是否需要预加载下一个号段
            if (!nextReady && !loadingNext.get() && currentSegment.getUsageRatio() >= PRELOAD_THRESHOLD) {
                // 异步预加载下一个号段
                if (loadingNext.compareAndSet(false, true)) {
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            nextSegment = segmentLoader.get();
                            nextReady = true;
                        } finally {
                            loadingNext.set(false);
                        }
                    });
                }
            }
            return value;
        }

        // 当前号段已耗尽，需要切换到下一个号段
        lock.lock();
        try {
            // 双重检查，可能其他线程已经切换了
            value = currentSegment.getNext();
            if (value != -1) {
                return value;
            }

            // 等待下一个号段准备好
            if (nextReady) {
                currentSegment = nextSegment;
                nextSegment = null;
                nextReady = false;
            } else {
                // 下一个号段还没准备好，同步加载
                currentSegment = segmentLoader.get();
            }

            // 从新号段获取值
            value = currentSegment.getNext();
            if (value == -1) {
                throw new RuntimeException("获取编号失败：新号段立即耗尽");
            }
            return value;
        } finally {
            lock.unlock();
        }
    }

    public String getOrgId() {
        return orgId;
    }
}
