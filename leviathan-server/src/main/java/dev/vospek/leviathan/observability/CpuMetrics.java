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
        this.processCpuLoad = REGISTRY.gauge("cpu.process.load", OS_BEAN::getProcessCpuLoad);
        this.systemCpuLoad = REGISTRY.gauge("cpu.system.load", OS_BEAN::getSystemLoadAverage);
        this.processCpuTime = REGISTRY.gaugeLong("cpu.process.time", () -> {
            // getProcessCpuTime 可能不被所有 JDK 支持
            try {
                return (Long) OS_BEAN.getClass().getMethod("getProcessCpuTime").invoke(OS_BEAN);
            } catch (Exception e) {
                return -1L;
            }
        });
        this.availableProcessors = REGISTRY.gauge("cpu.available", () -> Runtime.getRuntime().availableProcessors());
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
        return processCpuLoad.getAsDouble();
    }

    public double getSystemCpuLoad() {
        return systemCpuLoad.getAsDouble();
    }

    public long getProcessCpuTime() {
        return processCpuTime.getAsLong();
    }

    public int getAvailableProcessors() {
        return availableProcessors.getAsLong().intValue();
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