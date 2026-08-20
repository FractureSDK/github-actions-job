package dev.vospek.leviathan.observability;

import dev.vospek.leviathan.config.modules.misc.RegionFormatConfig;
import me.earthme.luminol.enums.EnumRegionFormat;

/**
 * 存储指标收集器
 * <p>
 * 暴露当前 region 格式配置状态（格式名、压缩级别、IO 线程数、冲刷延迟、虚拟线程），
 * 并注册 Chunk IO 延迟直方图骨架。
 * <p>
 * 对应 Phase 1-B (W2-01) Linear V2 — 0055 Metrics
 */
public final class StorageMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();

    private final MetricRegistry.Gauge<Long> isLinear;
    private final MetricRegistry.Gauge<Integer> compressionLevel;
    private final MetricRegistry.Gauge<Integer> ioThreadCount;
    private final MetricRegistry.Gauge<Integer> ioFlushDelayMs;
    private final MetricRegistry.Gauge<Integer> useLinearVirtualThread;

    // Chunk IO 延迟直方图骨架：待 NMS ChunkRegionLoader 接入后调用 recordNanos/recordMillis 打点
    private final MetricRegistry.Histogram regionReadLatency;
    private final MetricRegistry.Histogram regionWriteLatency;

    private StorageMetrics() {
        this.isLinear = REGISTRY.gaugeLong(
            "storage.region_format.is_linear", this::isLinearActive
        );
        this.compressionLevel = REGISTRY.gauge(
            "storage.region_format.compression_level", () -> RegionFormatConfig.compressionLevel
        );
        this.ioThreadCount = REGISTRY.gauge(
            "storage.region_format.io_threads", () -> RegionFormatConfig.ioThreadCount
        );
        this.ioFlushDelayMs = REGISTRY.gauge(
            "storage.region_format.io_flush_delay_ms", () -> RegionFormatConfig.ioFlushDelay
        );
        this.useLinearVirtualThread = REGISTRY.gauge(
            "storage.region_format.linear_virtual_thread", this::useLinearVirtualThread
        );
        this.regionReadLatency = REGISTRY.histogram("storage.region.read_latency_ns");
        this.regionWriteLatency = REGISTRY.histogram("storage.region.write_latency_ns");
    }

    private static final class Holder {
        static final StorageMetrics INSTANCE = new StorageMetrics();
    }

    public static StorageMetrics get() {
        return Holder.INSTANCE;
    }

    // ==================== 便捷查询 ====================

    public String getRegionFormatName() {
        return RegionFormatConfig.regionFormatName;
    }

    public EnumRegionFormat getRegionFormat() {
        return RegionFormatConfig.regionFormat;
    }

    public long isLinearActive() {
        EnumRegionFormat format = RegionFormatConfig.regionFormat;
        return format == EnumRegionFormat.LINEAR_V2 || format == EnumRegionFormat.B_LINEAR ? 1L : 0L;
    }

    public int getCompressionLevel() {
        return RegionFormatConfig.compressionLevel;
    }

    public int getIoThreadCount() {
        return RegionFormatConfig.ioThreadCount;
    }

    public int getIoFlushDelayMs() {
        return RegionFormatConfig.ioFlushDelay;
    }

    public int useLinearVirtualThread() {
        return RegionFormatConfig.linearUseVirtualThread ? 1 : 0;
    }

    public MetricRegistry.Histogram getRegionReadLatency() {
        return regionReadLatency;
    }

    public MetricRegistry.Histogram getRegionWriteLatency() {
        return regionWriteLatency;
    }

    /**
     * 存储配置摘要（用于诊断日志）
     */
    public String describe() {
        return "Region format: " + RegionFormatConfig.regionFormatName
            + ", compression level: " + RegionFormatConfig.compressionLevel
            + ", io threads: " + RegionFormatConfig.ioThreadCount
            + ", io flush delay: " + RegionFormatConfig.ioFlushDelay + "ms"
            + ", linear virtual thread: " + RegionFormatConfig.linearUseVirtualThread;
    }
}