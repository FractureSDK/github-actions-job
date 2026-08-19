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
    // 标准 JDK 实现都实现了 com.sun.management.OperatingSystemMXBean，直接强转避免反射
    private static final com.sun.management.OperatingSystemMXBean SUN_OS_BEAN =
        OS_BEAN instanceof com.sun.management.OperatingSystemMXBean ? (com.sun.management.OperatingSystemMXBean) OS_BEAN : null;

    private final MetricRegistry.Gauge<Double> processCpuLoad;
    private final MetricRegistry.Gauge<Double> systemCpuLoad;
    private final MetricRegistry.Gauge<Long> processCpuTime;
    private final MetricRegistry.Gauge<Integer> availableProcessors;

    private CpuMetrics() {
        this.processCpuLoad = REGISTRY.gaugeDouble("cpu.process.load", () -> getProcessCpuLoad());
        this.systemCpuLoad = REGISTRY.gaugeDouble("cpu.system.load", this::getSystemCpuLoad);
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

    /**
     * 获取进程 CPU 使用率（0.0 ~ 1.0），不可用时返回 0.0
     */
    public double getProcessCpuLoad() {
        if (SUN_OS_BEAN != null) {
            return Math.max(0.0, SUN_OS_BEAN.getProcessCpuLoad());
        }
        return 0.0;
    }

    /**
     * 获取系统 CPU 使用率（0.0 ~ 1.0），不可用时回退到 1 分钟负载平均值
     */
    public double getSystemCpuLoad() {
        if (SUN_OS_BEAN != null) {
            double load = SUN_OS_BEAN.getSystemCpuLoad();
            if (load >= 0) {
                return load;
            }
        }
        return OS_BEAN.getSystemLoadAverage();
    }

    /**
     * 获取进程累计 CPU 时间（纳秒），不可用时返回 -1
     */
    public long getProcessCpuTime() {
        if (SUN_OS_BEAN != null) {
            return SUN_OS_BEAN.getProcessCpuTime();
        }
        return -1L;
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
        return OS_BEAN.getSystemLoadAverage();
    }
}