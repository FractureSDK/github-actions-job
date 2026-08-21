package dev.vospek.leviathan.observability;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步 / 调度指标收集器（骨架）
 * <p>
 * 注册 Wave 6 异步卸载层指标。数据来源为 Chunk 异步 Save/Load、
 * Player Data、Scheduler 队列等 NMS 路径（当前工作树无应用化异步源码），
 * 故本类仅暴露 setter 与直方图，由后续源码应用后打点。
 * <p>
 * 对应 Phase 1-F (W6-01 Observer / W6-02 Async Save / W6-03 Async Load
 * / W6-04 Player Data / W6-05 Scheduler / W6-06 Monitoring) 可观测面
 */
public final class AsyncMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();

    // 队列深度 / 积压（由 NMS 异步队列 hook 设置）
    private final AtomicLong saveQueueDepth = new AtomicLong(0);
    private final AtomicLong loadQueueDepth = new AtomicLong(0);
    private final AtomicLong playerDataQueueDepth = new AtomicLong(0);
    private final AtomicLong deferredTaskCount = new AtomicLong(0);
    private final AtomicLong asyncTaskCount = new AtomicLong(0);
    private final AtomicLong syncTaskCount = new AtomicLong(0);

    // 错误 / 重试（W6-06 监控）
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong retryCount = new AtomicLong(0);

    // 队列等待直方图骨架：由 NMS 异步路径打点
    private final MetricRegistry.Histogram saveQueueWaitNs;
    private final MetricRegistry.Histogram loadQueueWaitNs;

    private AsyncMetrics() {
        REGISTRY.gaugeLong("async.save.queue_depth", this::getSaveQueueDepth);
        REGISTRY.gaugeLong("async.load.queue_depth", this::getLoadQueueDepth);
        REGISTRY.gaugeLong("async.player_data.queue_depth", this::getPlayerDataQueueDepth);
        REGISTRY.gaugeLong("async.scheduler.deferred_tasks", this::getDeferredTaskCount);
        REGISTRY.gaugeLong("async.scheduler.async_tasks", this::getAsyncTaskCount);
        REGISTRY.gaugeLong("async.scheduler.sync_tasks", this::getSyncTaskCount);
        REGISTRY.gaugeLong("async.error.count", this::getErrorCount);
        REGISTRY.gaugeLong("async.retry.count", this::getRetryCount);
        this.saveQueueWaitNs = REGISTRY.histogram("async.save.queue_wait_ns");
        this.loadQueueWaitNs = REGISTRY.histogram("async.load.queue_wait_ns");
    }

    private static final class Holder {
        static final AsyncMetrics INSTANCE = new AsyncMetrics();
    }

    public static AsyncMetrics get() {
        return Holder.INSTANCE;
    }

    // ==================== NMS hook setter ====================

    public void setSaveQueueDepth(long depth) {
        saveQueueDepth.set(depth);
    }

    public void setLoadQueueDepth(long depth) {
        loadQueueDepth.set(depth);
    }

    public void setPlayerDataQueueDepth(long depth) {
        playerDataQueueDepth.set(depth);
    }

    public void setDeferredTaskCount(long count) {
        deferredTaskCount.set(count);
    }

    public void setAsyncTaskCount(long count) {
        asyncTaskCount.set(count);
    }

    public void setSyncTaskCount(long count) {
        syncTaskCount.set(count);
    }

    public void addError() {
        errorCount.incrementAndGet();
    }

    public void addRetry() {
        retryCount.incrementAndGet();
    }

    public void recordSaveQueueWaitNanos(long nanos) {
        saveQueueWaitNs.recordNanos(nanos);
    }

    public void recordLoadQueueWaitNanos(long nanos) {
        loadQueueWaitNs.recordNanos(nanos);
    }

    // ==================== 便捷查询 ====================

    public long getSaveQueueDepth() {
        return saveQueueDepth.get();
    }

    public long getLoadQueueDepth() {
        return loadQueueDepth.get();
    }

    public long getPlayerDataQueueDepth() {
        return playerDataQueueDepth.get();
    }

    public long getDeferredTaskCount() {
        return deferredTaskCount.get();
    }

    public long getAsyncTaskCount() {
        return asyncTaskCount.get();
    }

    public long getSyncTaskCount() {
        return syncTaskCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public long getRetryCount() {
        return retryCount.get();
    }

    public MetricRegistry.Histogram getSaveQueueWaitNs() {
        return saveQueueWaitNs;
    }

    public MetricRegistry.Histogram getLoadQueueWaitNs() {
        return loadQueueWaitNs;
    }

    /**
     * 异步配置摘要（用于诊断日志）
     */
    public String describe() {
        return "Save queue: " + saveQueueDepth.get()
            + ", load queue: " + loadQueueDepth.get()
            + ", player data queue: " + playerDataQueueDepth.get()
            + ", scheduler (sync/async/deferred): " + syncTaskCount.get()
            + "/" + asyncTaskCount.get() + "/" + deferredTaskCount.get()
            + ", errors: " + errorCount.get();
    }
}