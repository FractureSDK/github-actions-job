package dev.vospek.leviathan.observability;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;

/**
 * JIT 编译指标收集器
 * <p>
 * 记录 JIT 编译器名称与累计编译时间。
 * <p>
 * 对应 Phase 1-A (W1-04) Runtime Diagnostics — JIT Status
 */
public final class JitMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();
    private static final CompilationMXBean COMPILATION_BEAN = ManagementFactory.getCompilationMXBean();

    private final MetricRegistry.Gauge<Long> totalCompilationTimeMs;

    private JitMetrics() {
        this.totalCompilationTimeMs = REGISTRY.gaugeLong(
            "jit.total_compilation_time_ms", this::getTotalCompilationTimeMs
        );
    }

    private static final class Holder {
        static final JitMetrics INSTANCE = new JitMetrics();
    }

    public static JitMetrics get() {
        return Holder.INSTANCE;
    }

    /**
     * JIT 编译器名称（HotSpot 下为 C1/C2 组合名，如 "HotSpot 64-Bit Tiered Compilers"）
     */
    public String getCompilerName() {
        if (COMPILATION_BEAN == null || COMPILATION_BEAN.getName() == null) {
            return "unknown";
        }
        return COMPILATION_BEAN.getName();
    }

    /**
     * 累计 JIT 编译时间（毫秒）；未开启编译时间监控时返回 -1
     */
    public long getTotalCompilationTimeMs() {
        if (COMPILATION_BEAN == null || !COMPILATION_BEAN.isCompilationTimeMonitoringSupported()) {
            return -1;
        }
        return COMPILATION_BEAN.getTotalCompilationTime();
    }
}