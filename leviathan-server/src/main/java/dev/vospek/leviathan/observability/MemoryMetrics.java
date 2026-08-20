package dev.vospek.leviathan.observability;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 内存指标收集器
 * <p>
 * 记录 Heap/Non-Heap、Direct Memory、GC Count/Time、Allocation Rate 等指标。
 * <p>
 * 对应 Phase 0-E: P0-014
 */
public final class MemoryMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();
    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();
    private static final OperatingSystemMXBean OS_BEAN =
        ManagementFactory.getOperatingSystemMXBean();
    private static final com.sun.management.OperatingSystemMXBean SUN_OS_BEAN =
        OS_BEAN instanceof com.sun.management.OperatingSystemMXBean
            ? (com.sun.management.OperatingSystemMXBean) OS_BEAN
            : null;

    // Unsafe 反射只做一次（BufferPoolMXBean 不可用时兜底）
    private static final Method UNSAFE_GET_DIRECT_MEMORY;
    private static final Method UNSAFE_MAX_DIRECT_MEMORY;
    private static final Object UNSAFE_INSTANCE;

    // JDK 11+ 提供 BufferPoolMXBean("direct")，优先使用；比 Unsafe 更准确且无需反射
    private static final BufferPoolMXBean DIRECT_POOL = findDirectBufferPool();

    private static BufferPoolMXBean findDirectBufferPool() {
        for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            if ("direct".equalsIgnoreCase(pool.getName())) {
                return pool;
            }
        }
        return null;
    }

    static {
        Method getDirectMemory = null;
        Method maxDirectMemory = null;
        Object unsafe = null;
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = field.get(null);
            getDirectMemory = unsafeClass.getMethod("getDirectMemory");
            maxDirectMemory = unsafeClass.getMethod("maxDirectMemory");
        } catch (Exception ignored) {
            // 反射不可用时指标返回 -1
        }
        UNSAFE_GET_DIRECT_MEMORY = getDirectMemory;
        UNSAFE_MAX_DIRECT_MEMORY = maxDirectMemory;
        UNSAFE_INSTANCE = unsafe;
    }

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

    // GC 指标（直接从 GarbageCollectorMXBean 读取，无需监听器）
    private final MetricRegistry.Gauge<Long> gcCount;
    private final MetricRegistry.Gauge<Long> gcTime;
    private final MetricRegistry.Gauge<Double> gcCpuPercent;

    // Allocation Rate
    private final MetricRegistry.Gauge<Double> allocationRate;
    private volatile long lastHeapUsed = 0;
    private volatile long lastCheckTime = System.nanoTime();

    private MemoryMetrics() {
        // Heap
        this.heapUsed = REGISTRY.gaugeLong(
            "memory.heap.used", () -> MEMORY_BEAN.getHeapMemoryUsage().getUsed()
        );
        this.heapMax = REGISTRY.gaugeLong(
            "memory.heap.max", () -> MEMORY_BEAN.getHeapMemoryUsage().getMax()
        );
        this.heapCommitted = REGISTRY.gaugeLong(
            "memory.heap.committed", () -> MEMORY_BEAN.getHeapMemoryUsage().getCommitted()
        );
        this.heapUsagePercent = REGISTRY.gaugeDouble(
            "memory.heap.usage_percent", this::calculateHeapUsagePercent
        );

        // Non-Heap
        this.nonHeapUsed = REGISTRY.gaugeLong(
            "memory.nonheap.used", () -> MEMORY_BEAN.getNonHeapMemoryUsage().getUsed()
        );
        this.nonHeapMax = REGISTRY.gaugeLong(
            "memory.nonheap.max", () -> MEMORY_BEAN.getNonHeapMemoryUsage().getMax()
        );
        this.nonHeapCommitted = REGISTRY.gaugeLong(
            "memory.nonheap.committed", () -> MEMORY_BEAN.getNonHeapMemoryUsage().getCommitted()
        );

        // Direct Memory (通过 Unsafe 估算)
        this.directMemoryUsed = REGISTRY.gaugeLong(
            "memory.direct.used", this::estimateDirectMemoryUsed
        );
        this.directMemoryMax = REGISTRY.gaugeLong(
            "memory.direct.max", this::estimateDirectMemoryMax
        );

        // GC
        this.gcCount = REGISTRY.gaugeLong("gc.total.count", this::calculateGcCount);
        this.gcTime = REGISTRY.gaugeLong("gc.total.time_ms", this::calculateGcTime);
        this.gcCpuPercent = REGISTRY.gaugeDouble("gc.cpu_percent", this::calculateGcCpuPercent);

        // Allocation Rate
        this.allocationRate = REGISTRY.gaugeDouble(
            "memory.allocation_rate_mb_s", this::calculateAllocationRate
        );

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
     * GC 计数与耗时直接从 MXBean 读取（累计值）
     */
    private long calculateGcCount() {
        long total = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gcBean.getCollectionCount();
            if (count >= 0) total += count;
        }
        return total;
    }

    private long calculateGcTime() {
        long total = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long time = gcBean.getCollectionTime();
            if (time >= 0) total += time;
        }
        return total;
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

    public long getGcCount() { return gcCount.getAsLong(); }
    public long getGcTime() { return gcTime.getAsLong(); }
    public double getGcCpuPercent() { return gcCpuPercent.getAsDouble(); }
    public double getAllocationRateMbPerSec() { return allocationRate.getAsDouble(); }

    // ==================== 内部计算方法 ====================

    private double calculateHeapUsagePercent() {
        long max = getHeapMax();
        if (max <= 0) return 0;
        return (getHeapUsed() * 100.0) / max;
    }

    private long estimateDirectMemoryUsed() {
        if (DIRECT_POOL != null) {
            return DIRECT_POOL.getMemoryUsed();
        }
        if (UNSAFE_GET_DIRECT_MEMORY == null || UNSAFE_INSTANCE == null) {
            return -1;
        }
        try {
            return (long) UNSAFE_GET_DIRECT_MEMORY.invoke(UNSAFE_INSTANCE);
        } catch (Exception e) {
            return -1;
        }
    }

    private long estimateDirectMemoryMax() {
        if (DIRECT_POOL != null) {
            return DIRECT_POOL.getTotalCapacity();
        }
        if (UNSAFE_MAX_DIRECT_MEMORY == null || UNSAFE_INSTANCE == null) {
            return -1;
        }
        try {
            return (long) UNSAFE_MAX_DIRECT_MEMORY.invoke(UNSAFE_INSTANCE);
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
        if (SUN_OS_BEAN != null) {
            return SUN_OS_BEAN.getProcessCpuTime();
        }
        return -1;
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
        List<MemoryPoolInfo> result = new ArrayList<>(pools.size());

        for (MemoryPoolMXBean pool : pools) {
            MemoryUsage usage = pool.getUsage();
            if (usage == null) {
                continue; // 某些池在无效/未使用时返回 null
            }
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