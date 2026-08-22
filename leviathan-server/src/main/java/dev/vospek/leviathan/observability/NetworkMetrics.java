package dev.vospek.leviathan.observability;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 网络指标收集器（骨架）
 * <p>
 * 注册 Wave 3 / Wave 4 网络层指标。数据来源为 Netty / NMS 数据包路径，
 * 当前工作树无应用化 NMS 源码，故本类仅暴露 setter 与直方图，由后续
 * 源码应用后在 Connection / Channel 路径打点。
 * <p>
 * 对应 Phase 1-C (W3-01/W3-02/W4-01/W4-03) 网络可观测面
 */
public final class NetworkMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();

    // 连接 / 吞吐（由 NMS ConnectionServer / Netty hook 设置）
    private final AtomicLong connectionCount = new AtomicLong(0);
    private final AtomicLong bytesSent = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicLong packetsSent = new AtomicLong(0);
    private final AtomicLong packetsReceived = new AtomicLong(0);

    // 压缩配置（W3-02 骨架；feature flag 由 CoreConfig.featureZstdNetwork 决定生效态）
    private volatile int compressionThreshold = -1;
    private volatile int compressionLevel = -1;
    private volatile long zstdEnabled = 0;

    // 延迟直方图骨架：由 NMS 网络路径调用 recordNanos / recordMillis 打点
    private final MetricRegistry.Histogram packetSendLatency;
    private final MetricRegistry.Histogram compressionTime;
    private final MetricRegistry.Histogram rttMs;

    private NetworkMetrics() {
        REGISTRY.gaugeLong("network.connection.count", this::getConnectionCount);
        REGISTRY.gaugeLong("network.bytes_sent", bytesSent::get);
        REGISTRY.gaugeLong("network.bytes_received", bytesReceived::get);
        REGISTRY.gaugeLong("network.packets_sent", packetsSent::get);
        REGISTRY.gaugeLong("network.packets_received", packetsReceived::get);
        REGISTRY.gaugeInt("network.compression.threshold_bytes", () -> compressionThreshold);
        REGISTRY.gaugeInt("network.compression.level", () -> compressionLevel);
        REGISTRY.gaugeLong("network.compression.zstd_enabled", () -> zstdEnabled);
        this.packetSendLatency = REGISTRY.histogram("network.packet.send_latency_ns");
        this.compressionTime = REGISTRY.histogram("network.compression.time_us");
        this.rttMs = REGISTRY.histogram("network.rtt_ms");
    }

    private static final class Holder {
        static final NetworkMetrics INSTANCE = new NetworkMetrics();
    }

    public static NetworkMetrics get() {
        return Holder.INSTANCE;
    }

    // ==================== NMS hook setter ====================

    public void setConnectionCount(long count) {
        connectionCount.set(count);
    }

    public void addBytesSent(long delta) {
        bytesSent.addAndGet(delta);
    }

    public void addBytesReceived(long delta) {
        bytesReceived.addAndGet(delta);
    }

    public void addPacketsSent(long delta) {
        packetsSent.addAndGet(delta);
    }

    public void addPacketsReceived(long delta) {
        packetsReceived.addAndGet(delta);
    }

    public void setCompressionThreshold(int threshold) {
        compressionThreshold = threshold;
    }

    public void setCompressionLevel(int level) {
        compressionLevel = level;
    }

    public void setZstdEnabled(boolean enabled) {
        zstdEnabled = enabled ? 1L : 0L;
    }

    public void recordPacketSendLatencyNanos(long nanos) {
        packetSendLatency.recordNanos(nanos);
    }

    public void recordCompressionTimeMicros(double micros) {
        compressionTime.record(micros);
    }

    public void recordRttMillis(double millis) {
        rttMs.recordMillis(millis);
    }

    // ==================== 便捷查询 ====================

    public long getConnectionCount() {
        return connectionCount.get();
    }

    public long getBytesSent() {
        return bytesSent.get();
    }

    public long getBytesReceived() {
        return bytesReceived.get();
    }

    public long getPacketsSent() {
        return packetsSent.get();
    }

    public long getPacketsReceived() {
        return packetsReceived.get();
    }

    public int getCompressionThreshold() {
        return compressionThreshold;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public boolean isZstdEnabled() {
        return zstdEnabled == 1L;
    }

    public MetricRegistry.Histogram getPacketSendLatency() {
        return packetSendLatency;
    }

    public MetricRegistry.Histogram getCompressionTime() {
        return compressionTime;
    }

    public MetricRegistry.Histogram getRttMs() {
        return rttMs;
    }

    /**
     * 网络配置摘要（用于诊断日志）
     */
    public String describe() {
        return "Connections: " + connectionCount.get()
            + ", packets sent: " + packetsSent.get()
            + ", packets recv: " + packetsReceived.get()
            + ", zstd: " + (zstdEnabled == 1L ? "on" : "off")
            + ", compression threshold: " + compressionThreshold;
    }
}