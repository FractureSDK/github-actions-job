package dev.vospek.leviathan.observability;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程指标收集器
 * <p>
 * 记录线程数量、状态分布、CPU 时间等指标。
 * <p>
 * 对应 Phase 0-E: P0-015
 */
public final class ThreadMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();
    private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();

    private final MetricRegistry.Gauge<Integer> threadCount;
    private final MetricRegistry.Gauge<Integer> peakThreadCount;
    private final MetricRegistry.Gauge<Integer> daemonThreadCount;
    private final MetricRegistry.Gauge<Long> totalThreadCpuTime;
    private final MetricRegistry.Gauge<Map<String, Long>> threadStateDistribution;

    // 线程名 -> CPU 时间（用于热点分析）
    private final Map<String, Long> threadCpuTimes = new ConcurrentHashMap<>();

    private ThreadMetrics() {
        this.threadCount = REGISTRY.gauge("thread.count", THREAD_BEAN::getThreadCount);
        this.peakThreadCount = REGISTRY.gauge("thread.peak", THREAD_BEAN::getPeakThreadCount);
        this.daemonThreadCount = REGISTRY.gauge("thread.daemon", THREAD_BEAN::getDaemonThreadCount);
        this.totalThreadCpuTime = REGISTRY.gaugeLong("thread.total_cpu_ns", THREAD_BEAN::getTotalThreadCpuTime);
        this.threadStateDistribution = REGISTRY.gauge("thread.states", this::getThreadStateDistribution);
    }

    private static final class Holder {
        static final ThreadMetrics INSTANCE = new ThreadMetrics();
    }

    public static ThreadMetrics get() {
        return Holder.INSTANCE;
    }

    /**
     * 记录指定线程的 CPU 时间（用于热点分析）
     */
    public void recordThreadCpuTime(String threadName, long cpuTimeNanos) {
        threadCpuTimes.put(threadName, cpuTimeNanos);
    }

    /**
     * 获取线程状态分布
     */
    public Map<String, Long> getThreadStateDistribution() {
        Map<String, Long> distribution = new ConcurrentHashMap<>();
        ThreadInfo[] infos = THREAD_BEAN.dumpAllThreads(false, false);

        for (ThreadInfo info : infos) {
            String state = info.getThreadState().toString();
            distribution.merge(state, 1L, Long::sum);
        }

        return distribution;
    }

    /**
     * 获取 CPU 时间最高的 N 个线程
     */
    public List<ThreadCpuInfo> getTopCpuThreads(int limit) {
        List<ThreadCpuInfo> result = new ArrayList<>();

        // 先更新缓存
        long[] ids = THREAD_BEAN.getAllThreadIds();
        long[] cpuTimes = THREAD_BEAN.getThreadCpuTime(ids);
        ThreadInfo[] infos = THREAD_BEAN.getThreadInfo(ids);

        for (int i = 0; i < ids.length; i++) {
            if (infos[i] != null && cpuTimes[i] > 0) {
                result.add(new ThreadCpuInfo(
                    infos[i].getThreadName(),
                    infos[i].getThreadId(),
                    infos[i].getThreadState().toString(),
                    cpuTimes[i]
                ));
            }
        }

        result.sort((a, b) -> Long.compare(b.cpuTimeNanos(), a.cpuTimeNanos()));
        return result.subList(0, Math.min(limit, result.size()));
    }

    // ==================== 便捷查询 ====================

    public int getThreadCount() { return threadCount.getAsLong().intValue(); }
    public int getPeakThreadCount() { return peakThreadCount.getAsLong().intValue(); }
    public int getDaemonThreadCount() { return daemonThreadCount.getAsLong().intValue(); }
    public long getTotalThreadCpuTime() { return totalThreadCpuTime.getAsLong(); }
    public Map<String, Long> getThreadStates() { return threadStateDistribution.getValue(); }

    public record ThreadCpuInfo(
        String name,
        long id,
        String state,
        long cpuTimeNanos
    ) {}
}