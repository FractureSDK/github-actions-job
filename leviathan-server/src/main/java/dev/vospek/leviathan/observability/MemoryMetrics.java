package dev.vospek.leviathan.observability;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

/**
 * 内存指标收集器
 * <p>
 * 记录 Heap Used/Max/Committed、Direct Memory、Native Memory、GC Count/Pause、Allocation Rate 等指标。
 * <p>
 * 对应 Phase 0-E: P0-014
 */
public final class MemoryMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();
    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();

    // Heap 指标
    private final MetricRegistry.Gauge<Long> heapUsed;
    private final MetricRegistry.Gauge<Long> heapMax;
    private final MetricRegistry.Gauge<Long> heapCommitted;
    private final MetricRegistry.Gauge<Double> heapUsagePercent;

    // Non-Heap 指标
    private final MetricRegistry.Gauge<Long> nonHeapUsed;
    private final MetricRegistry.Gauge<Long> nonHeapMax;
    private final MetricRegistry.Gauge<Long> nonHeapCommitted;

    // Direct Memory 指标
    private final MetricRegistry.Gauge<Long> directMemoryUsed;
    private final MetricRegistry.Gauge<Long> directMemoryMax;

    // GC 指标
    private final MetricRegistry.Counter gcCount;
    private final MetricRegistry.Counter gcTime;
    private final MetricRegistry.Gauge<Long> lastGcDuration;
    private final MetricRegistry.Gauge<Double> gcCpuPercent;

    // Allocation Rate
    private final MetricRegistry.Gauge<Double> allocationRate;
    private long lastHeapUsed = 0;
    private long lastCheckTime = System.nanoTime();

    private MemoryMetrics() {
        // Heap
        this.heapUsed = REGISTRY.gauge("memory.heap.used", () -> MEMORY_BEAN.getHeapMemoryUsage().getUsed());
        this.heapMax = REGISTRY.gauge("memory.heap.max", () -> MEMORY_BEAN.getHeapMemoryUsage().getMax());
        this.heapCommitted = REGISTRY.gauge("memory.heap.committed", () -> MEMORY_BEAN.getHeapMemoryUsage().getCommitted());
        this.heapUsagePercent = REGISTRY.gauge("memory.heap.usage_percent", this::calculateHeapUsagePercent);

        // Non-Heap
        this.nonHeapUsed = REGISTRY.gauge("memory.nonheap.used", () -> MEMORY_BEAN.getNonHeapMemoryUsage().getUsed());
        this.nonHeapMax = REGISTRY.gauge("memory.nonheap.max", () -> MEMORY_BEAN.getNonHeapMemoryUsage().getMax());
        this.nonHeapCommitted = REGISTRY.gauge("memory.nonheap.committed", () -> MEMORY_BEAN.getNonHeapMemoryUsage().getCommitted());

        // Direct Memory (通过 Unsafe 估算)
        this.directMemoryUsed = REGISTRY.gaugeLong("memory.direct.used", this::estimateDirectMemoryUsed);
        this.directMemoryMax = REGISTRY.gaugeLong("memory.direct.max", this::estimateDirectMemoryMax);

        // GC
        this.gcCount = REGISTRY.counter("gc.total.count");
        this.gcTime = REGISTRY.counter("gc.total.time_ms");
        this.lastGcDuration = REGISTRY.gaugeLong("gc.last.duration_ms", () -> 0); // 需要 GC 监听器更新
        this.gcCpuPercent = REGISTRY.gauge("gc.cpu_percent", this::calculateGcCpuPercent);

        // Allocation Rate
        this.allocationRate = REGISTRY.gauge("memory.allocation_rate_mb_s", this::calculateAllocationRate);

        // 初始化基线
        this.lastHeapUsed = MEMORY_BEAN.getHeapMemoryUsage().getUsed();
        this.lastCheckTime = System.nanoTime();
    }

    private static final class Holder {
        static final MemoryMetrics INSTANCE = new MemoryMetrics();
    }

    public static MemoryMetrics get() {
        return Holder.INSTANCE;
    }

    /**
     * 记录 GC 事件（由 GC 监听器调用）
     */
    public void recordGc(long durationMs) {
        gcCount.inc();
        gcTime.inc(durationMs);
        // lastGcDuration 会通过 Gauge 暴露，这里不直接设置
    }

    // ==================== 便捷查询 ====================

    public long getHeapUsed() { return heapUsed.getAsLong(); }
    public long getHeapMax() { return heapMax.getAsLong(); }
    public long getHeapCommitted() { return heapCommitted.getAsLong(); }
    public double getHeapUsagePercent() { return heapUsagePercent.getAsDouble(); }

    public long getNonHeapUsed() { return nonHeapUsed.getAsLong(); }
    public long getNonHeapMax() { return nonHeapMax.getAsLong(); }
    public long getNonHeapCommitted() { return nonHeapCommitted.getAsLong(); }

    public long getDirectMemoryUsed() { return directMemoryUsed.getAsLong(); }
    public long getDirectMemoryMax() { return directMemoryMax.getAsLong(); }

    public long getGcCount() { return gcCount.get(); }
    public long getGcTime() { return gcTime.get(); }
    public double getGcCpuPercent() { return gcCpuPercent.getAsDouble(); }
    public double getAllocationRateMbPerSec() { return allocationRate.getAsDouble(); }

    // ==================== 内部计算方法 ====================

    private double calculateHeapUsagePercent() {
        long max = getHeapMax();
        if (max <= 0) return 0;
        return (getHeapUsed() * 100.0) / max;
    }

    private long estimateDirectMemoryUsed() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            java.lang.reflect.Method getDirectMemory = unsafeClass.getMethod("getDirectMemory");
            return (long) getDirectMemory.invoke(unsafe);
        } catch (Exception e) {
            return -1;
        }
    }

    private long estimateDirectMemoryMax() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            java.lang.reflect.Method maxDirectMemory = unsafeClass.getMethod("maxDirectMemory");
            return (long) maxDirectMemory.invoke(unsafe);
        } catch (Exception e) {
            return -1;
        }
    }

    private double calculateGcCpuPercent() {
        // 简化：返回 GC 时间占总 CPU 时间的比例
        long totalGcTime = getGcTime();
        long processCpuTime = getProcessCpuTimeNanos();
        if (processCpuTime <= 0) return 0;
        return (totalGcTime * 1_000_000.0 / processCpuTime) * 100.0;
    }

    private long getProcessCpuTimeNanos() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            return (Long) osBean.getClass().getMethod("getProcessCpuTime").invoke(osBean);
        } catch (Exception e) {
            return -1;
        }
    }

    private double calculateAllocationRate() {
        long now = System.nanoTime();
        long currentHeapUsed = MEMORY_BEAN.getHeapMemoryUsage().getUsed();

        long elapsedNanos = now - lastCheckTime;
        if (elapsedNanos <= 0) return 0;

        long allocated = currentHeapUsed - lastHeapUsed;
        if (allocated < 0) allocated = 0; // GC 发生后可能减少

        lastHeapUsed = currentHeapUsed;
        lastCheckTime = now;

        // 转换为 MB/s
        return (allocated / (1024.0 * 1024.0)) / (elapsedNanos / 1_000_000_000.0);
    }

    // ==================== 内存池详细信息 ====================

    /**
     * 获取所有内存池使用情况
     */
    public List<MemoryPoolInfo> getMemoryPools() {
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        List<MemoryPoolInfo> result = new java.util.ArrayList<>(pools.size());

        for (MemoryPoolMXBean pool : pools) {
            MemoryUsage usage = pool.getUsage();
            result.add(new MemoryPoolInfo(
                pool.getName(),
                pool.getType().toString(),
                usage.getUsed(),
                usage.getMax(),
                usage.getCommitted(),
                usage.getInit()
            ));
        }
        return result;
    }

    public record MemoryPoolInfo(
        String name,
        String type,
        long used,
        long max,
        long committed,
        long init
    ) {}
}