package dev.vospek.leviathan.observability;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * CPU 指标收集器
 * <p>
 * 记录进程 CPU、系统 CPU、主线程 CPU、工作线程 CPU 等指标。
 * <p>
 * 对应 Phase 0-E: P0-013
 */
public final class CpuMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();
    private static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getOperatingSystemMXBean();

    private final MetricRegistry.Gauge<Double> processCpuLoad;
    private final MetricRegistry.Gauge<Double> systemCpuLoad;
    private final MetricRegistry.Gauge<Long> processCpuTime;
    private final MetricRegistry.Gauge<Integer> availableProcessors;

    private CpuMetrics() {
        this.processCpuLoad = REGISTRY.gaugeDouble("cpu.process.load", () -> getProcessCpuLoad());
        this.systemCpuLoad = REGISTRY.gaugeDouble("cpu.system.load", OS_BEAN::getSystemLoadAverage);
        this.processCpuTime = REGISTRY.gaugeLong("cpu.process.time", this::getProcessCpuTime);
        this.availableProcessors = REGISTRY.gaugeInt("cpu.available", Runtime.getRuntime()::availableProcessors);
    }

    private static final class Holder {
        static final CpuMetrics INSTANCE = new CpuMetrics();
    }

    public static CpuMetrics get() {
        return Holder.INSTANCE;
    }

    /**
     * 手动刷新指标（Gauge 是懒加载的，通常不需要调用）
     */
    public void refresh() {
        // Gauge 会在获取时自动计算
    }

    // ==================== 便捷查询 ====================

    public double getProcessCpuLoad() {
        try {
            Double load = (Double) OS_BEAN.getClass().getMethod("getProcessCpuLoad").invoke(OS_BEAN);
            return load != null ? load : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double getSystemCpuLoad() {
        return OS_BEAN.getSystemLoadAverage();
    }

    public long getProcessCpuTime() {
        try {
            Long time = (Long) OS_BEAN.getClass().getMethod("getProcessCpuTime").invoke(OS_BEAN);
            return time != null ? time : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    public int getAvailableProcessors() {
        return availableProcessors.getAsInt();
    }

    /**
     * 获取进程 CPU 使用率百分比
     */
    public double getProcessCpuPercent() {
        return getProcessCpuLoad() * 100.0;
    }

    /**
     * 获取系统负载平均值（1分钟）
     */
    public double getSystemLoadAverage() {
        return getSystemCpuLoad();
    }
}