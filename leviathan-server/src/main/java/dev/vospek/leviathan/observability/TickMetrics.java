package dev.vospek.leviathan.observability;

import ca.spottedleaf.common.time.TickData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

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
    private final Map<String, WorldTickMetrics> worldMetrics = new ConcurrentHashMap<>();

    // 服务器级同步窗口缓存: suffix -> [avg, min, max] ms
    private final Map<String, double[]> serverSyncCache = new ConcurrentHashMap<>();

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
        syncTickData(server, server.tickTimes5s, "5s");
        syncTickData(server, server.tickTimes10s, "10s");
        syncTickData(server, server.tickTimes1m, "1m");

        // 同步世界级指标
        for (ServerLevel level : server.getAllLevels()) {
            WorldTickMetrics wm = getWorldMetrics(level.getWorld().getName());
            wm.syncFromTickData(level.tickTimes5s, "5s");
            wm.syncFromTickData(level.tickTimes10s, "10s");
            wm.syncFromTickData(level.tickTimes1m, "1m");
        }
    }

    private void syncTickData(MinecraftServer server, TickData tickData, String suffix) {
        TickData.TickReportData report = tickData.generateTickReport(
            null, System.nanoTime(), server.tickRateManager().nanosecondsPerTick()
        );
        if (report != null) {
            // 每次写入新数组并通过 put 发布，保证读取线程看到一致快照
            var segment = report.timePerTickData().segmentAll();
            double[] values = {
                segment.average() * 1.0E-6,
                segment.least() * 1.0E-6,
                segment.greatest() * 1.0E-6
            };
            serverSyncCache.put(suffix, values);

            REGISTRY.gaugeDouble(
                "tick.server." + suffix + ".avg", () -> syncWindowValue(serverSyncCache, suffix, 0)
            );
            REGISTRY.gaugeDouble(
                "tick.server." + suffix + ".min", () -> syncWindowValue(serverSyncCache, suffix, 1)
            );
            REGISTRY.gaugeDouble(
                "tick.server." + suffix + ".max", () -> syncWindowValue(serverSyncCache, suffix, 2)
            );
        }
    }

    /**
     * 读取同步窗口缓存的指定下标值，无数据时返回 0
     */
    private static double syncWindowValue(Map<String, double[]> cache, String suffix, int index) {
        double[] values = cache.get(suffix);
        return values != null ? values[index] : 0.0;
    }

    /**
     * 获取 5s 同步窗口的平均 MSPT（毫秒），无数据时返回 0
     */
    public double getSyncedMSPT() {
        double[] v = serverSyncCache.get("5s");
        return v != null ? v[0] : 0.0;
    }

    /**
     * 获取 5s 同步窗口的最小 MSPT（毫秒），无数据时返回 0
     */
    public double getSyncedMinMSPT() {
        double[] v = serverSyncCache.get("5s");
        return v != null ? v[1] : 0.0;
    }

    /**
     * 获取 5s 同步窗口的最大 MSPT（毫秒），无数据时返回 0
     */
    public double getSyncedMaxMSPT() {
        double[] v = serverSyncCache.get("5s");
        return v != null ? v[2] : 0.0;
    }

    /**
     * 基于 5s 同步窗口计算 TPS，无数据时返回 20.0
     */
    public double getSyncedTPS() {
        double[] v = serverSyncCache.get("5s");
        double avgMs = v != null ? v[0] : 0.0;
        return avgMs > 0 ? Math.min(1000.0 / avgMs, 20.0) : 20.0;
    }

    private double calculateTPS() {
        long count = serverTickTimer.getCount();
        if (count == 0) {
            return getSyncedTPS();
        }
        double avgMs = serverTickTimer.getMean();
        return avgMs > 0 ? Math.min(1000.0 / avgMs, 20.0) : 20.0;
    }

    private double calculateMSPT() {
        if (serverTickTimer.getCount() == 0) {
            return getSyncedMSPT();
        }
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

    // 百分位数查询（单位：微秒）
    public double getP50() {
        return serverTickTimer.p50();
    }

    public double getP95() {
        return serverTickTimer.p95();
    }

    public double getP99() {
        return serverTickTimer.p99();
    }

    public double getMax() {
        return serverTickTimer.getMax();
    }

    // ==================== 世界级指标内部类 ====================

    /**
     * 世界级 Tick 指标
     * <p>
     * 每个世界独立维护耗时统计与同步窗口缓存。
     */
    public static final class WorldTickMetrics {
        private final String worldName;
        private final MetricRegistry.Timer tickTimer;
        private final MetricRegistry.Counter overrunCounter;
        private final MetricRegistry.Counter spikeCounter;
        // 同步窗口缓存: suffix -> [avg, min, max] ms
        private final Map<String, double[]> syncCache = new ConcurrentHashMap<>();

        private WorldTickMetrics(String worldName) {
            this.worldName = worldName;
            this.tickTimer = REGISTRY.timer("tick.world", "world=" + worldName);
            this.overrunCounter = REGISTRY.counter("tick.world.overrun", "world=" + worldName);
            this.spikeCounter = REGISTRY.counter("tick.world.spike", "world=" + worldName);
        }

        public void recordTick(long nanos) {
            tickTimer.recordNanos(nanos);
            double ms = nanos / 1_000_000.0;
            if (ms > OVERRUN_THRESHOLD_MS) {
                overrunCounter.inc();
            }
            if (ms > SPIKE_THRESHOLD_MS) {
                spikeCounter.inc();
            }
        }

        public void syncFromTickData(MinecraftServer.TickTimes tickTimes, String suffix) {
            long[] times = tickTimes.getTimes();
            long min = Long.MAX_VALUE;
            long max = 0L;
            long total = 0L;
            int count = 0;
            for (long value : times) {
                if (value > 0L) {
                    count++;
                    if (value < min) min = value;
                    if (value > max) max = value;
                    total += value;
                }
            }
            if (count > 0) {
                double[] values = {
                    (total / (double) count) * 1.0E-6,
                    min * 1.0E-6,
                    max * 1.0E-6
                };
                syncCache.put(suffix, values);
            }

            REGISTRY.gaugeDouble(
                "tick.world." + worldName + "." + suffix + ".avg",
                () -> syncWindowValue(syncCache, suffix, 0)
            );
            REGISTRY.gaugeDouble(
                "tick.world." + worldName + "." + suffix + ".min",
                () -> syncWindowValue(syncCache, suffix, 1)
            );
            REGISTRY.gaugeDouble(
                "tick.world." + worldName + "." + suffix + ".max",
                () -> syncWindowValue(syncCache, suffix, 2)
            );
        }

        public double getTPS() {
            long count = tickTimer.getCount();
            if (count == 0) {
                return 20.0;
            }
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