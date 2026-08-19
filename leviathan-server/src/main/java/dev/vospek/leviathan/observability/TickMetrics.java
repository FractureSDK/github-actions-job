package dev.vospek.leviathan.observability;

import ca.spottedleaf.common.time.TickData;
import ca.spottedleaf.common.time.TickTimes;
import net.minecraft.server.MinecraftServer;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tick 性能指标收集器
 * <p>
 * 收集 TPS、MSPT、Tick Duration、Overrun、Spike 等指标，支持 P50/P95/P99/MAX 百分位数。
 * <p>
 * 对应 Phase 0-E: P0-012
 */
public final class TickMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();

    // 服务器级指标
    private final MetricRegistry.Timer serverTickTimer;
    private final MetricRegistry.Counter tickOverrunCounter;
    private final MetricRegistry.Counter tickSpikeCounter;
    private final MetricRegistry.Gauge<Double> currentTPS;
    private final MetricRegistry.Gauge<Double> currentMSPT;

    // Per-world 指标（懒加载）
    private final java.util.Map<String, WorldTickMetrics> worldMetrics = new java.util.concurrent.ConcurrentHashMap<>();

    // 阈值配置
    private static final double OVERRUN_THRESHOLD_MS = 50.0; // 50ms 视为 overrun
    private static final double SPIKE_THRESHOLD_MS = 100.0;  // 100ms 视为 spike

    private TickMetrics() {
        this.serverTickTimer = REGISTRY.timer("tick.server");
        this.tickOverrunCounter = REGISTRY.counter("tick.overrun");
        this.tickSpikeCounter = REGISTRY.counter("tick.spike");
        this.currentTPS = REGISTRY.gauge("tick.tps", () -> calculateTPS());
        this.currentMSPT = REGISTRY.gauge("tick.mspt", () -> calculateMSPT());
    }

    private static final class Holder {
        static final TickMetrics INSTANCE = new TickMetrics();
    }

    public static TickMetrics get() {
        return Holder.INSTANCE;
    }

    /**
     * 记录一次服务器 Tick 耗时（纳秒）
     */
    public void recordServerTick(long nanos) {
        serverTickTimer.recordNanos(nanos);

        double ms = nanos / 1_000_000.0;
        if (ms > OVERRUN_THRESHOLD_MS) {
            tickOverrunCounter.inc();
        }
        if (ms > SPIKE_THRESHOLD_MS) {
            tickSpikeCounter.inc();
        }
    }

    /**
     * 获取或创建世界级 Tick 指标
     */
    public WorldTickMetrics getWorldMetrics(String worldName) {
        return worldMetrics.computeIfAbsent(worldName, WorldTickMetrics::new);
    }

    /**
     * 记录世界 Tick 耗时
     */
    public void recordWorldTick(String worldName, long nanos) {
        getWorldMetrics(worldName).recordTick(nanos);
    }

    /**
     * 从 MinecraftServer 的 TickData 同步指标（用于兼容现有监控）
     */
    public void syncFromServer(MinecraftServer server) {
        // 同步服务器级指标
        syncTickData(server.tickTimes5s, "5s");
        syncTickData(server.tickTimes10s, "10s");
        syncTickData(server.tickTimes1m, "1m");

        // 同步世界级指标
        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            WorldTickMetrics wm = getWorldMetrics(level.getWorld().getName());
            wm.syncFromTickData(level.tickTimes5s, "5s");
            wm.syncFromTickData(level.tickTimes10s, "10s");
            wm.syncFromTickData(level.tickTimes1m, "1m");
        }
    }

    private void syncTickData(TickTimes tickTimes, String suffix) {
        TickData tickData = tickTimes.getTimes();
        TickData.TickReportData report = tickData.generateTickReport(null, System.nanoTime(), MinecraftServer.getServer().tickRateManager().nanosecondsPerTick());
        if (report != null) {
            double avgMs = report.timePerTickData().segmentAll().average() * 1.0E-6;
            double minMs = report.timePerTickData().segmentAll().least() * 1.0E-6;
            double maxMs = report.timePerTickData().segmentAll().greatest() * 1.0E-6;

            REGISTRY.gauge("tick.server." + suffix + ".avg", () -> avgMs);
            REGISTRY.gauge("tick.server." + suffix + ".min", () -> minMs);
            REGISTRY.gauge("tick.server." + suffix + ".max", () -> maxMs);
        }
    }

    private double calculateTPS() {
        long count = serverTickTimer.getCount();
        if (count == 0) return 20.0;
        double avgMs = serverTickTimer.getMean();
        return avgMs > 0 ? Math.min(1000.0 / avgMs, 20.0) : 20.0;
    }

    private double calculateMSPT() {
        return serverTickTimer.getMean();
    }

    // ==================== 便捷查询方法 ====================

    public double getCurrentTPS() {
        return currentTPS.getAsDouble();
    }

    public double getCurrentMSPT() {
        return currentMSPT.getAsDouble();
    }

    public long getOverrunCount() {
        return tickOverrunCounter.get();
    }

    public long getSpikeCount() {
        return tickSpikeCounter.get();
    }

    public MetricRegistry.Timer getServerTickTimer() {
        return serverTickTimer;
    }

    // 百分位数查询
    public double getP50() { return serverTickTimer.p50(); }
    public double getP95() { return serverTimer().p95(); }
    public double getP99() { return serverTimer().p99(); }
    public double getMax() { return serverTimer().getMax(); }

    private MetricRegistry.Timer serverTimer() {
        return serverTickTimer;
    }

    // ==================== 世界级指标内部类 ====================

    public static final class WorldTickMetrics {
        private final String worldName;
        private final MetricRegistry.Timer tickTimer;
        private final MetricRegistry.Counter overrunCounter;
        private final MetricRegistry.Counter spikeCounter;

        private WorldTickMetrics(String worldName) {
            this.worldName = worldName;
            this.tickTimer = REGISTRY.timer("tick.world", "world=" + worldName);
            this.overrunCounter = REGISTRY.counter("tick.world.overrun", "world=" + worldName);
            this.spikeCounter = REGISTRY.counter("tick.world.spike", "world=" + worldName);
        }

        public void recordTick(long nanos) {
            tickTimer.recordNanos(nanos);
            double ms = nanos / 1_000_000.0;
            if (ms > OVERRUN_THRESHOLD_MS) overrunCounter.inc();
            if (ms > SPIKE_THRESHOLD_MS) spikeCounter.inc();
        }

        public void syncFromTickData(TickTimes tickTimes, String suffix) {
            TickData tickData = tickTimes.getTimes();
            TickData.TickReportData report = tickData.generateTickReport(null, System.nanoTime(), MinecraftServer.getServer().tickRateManager().nanosecondsPerTick());
            if (report != null) {
                double avgMs = report.timePerTickData().segmentAll().average() * 1.0E-6;
                double minMs = report.timePerTickData().segmentAll().least() * 1.0E-6;
                double maxMs = report.timePerTickData().segmentAll().greatest() * 1.0E-6;

                REGISTRY.gauge("tick.world." + worldName + "." + suffix + ".avg", () -> avgMs);
                REGISTRY.gauge("tick.world." + worldName + "." + suffix + ".min", () -> minMs);
                REGISTRY.gauge("tick.world." + worldName + "." + suffix + ".max", () -> maxMs);
            }
        }

        public double getTPS() {
            long count = tickTimer.getCount();
            if (count == 0) return 20.0;
            double avgMs = tickTimer.getMean();
            return avgMs > 0 ? Math.min(1000.0 / avgMs, 20.0) : 20.0;
        }

        public double getMSPT() { return tickTimer.getMean(); }
        public double getP50() { return tickTimer.p50(); }
        public double getP95() { return tickTimer.p95(); }
        public double getP99() { return tickTimer.p99(); }
        public double getMax() { return tickTimer.getMax(); }
        public long getOverrunCount() { return overrunCounter.get(); }
        public long getSpikeCount() { return spikeCounter.get(); }
    }
}