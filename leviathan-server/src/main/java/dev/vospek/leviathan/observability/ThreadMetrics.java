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
    private final MetricRegistry.Gauge<Integer> threadStateCount; // Track runnable count as example

    // 线程名 -> CPU 时间（用于热点分析）
    private final Map<String, Long> threadCpuTimes = new ConcurrentHashMap<>();

    private ThreadMetrics() {
        this.threadCount = REGISTRY.gaugeInt("thread.count", THREAD_BEAN::getThreadCount);
        this.peakThreadCount = REGISTRY.gaugeInt("thread.peak", THREAD_BEAN::getPeakThreadCount);
        this.daemonThreadCount = REGISTRY.gaugeInt("thread.daemon", THREAD_BEAN::getDaemonThreadCount);
        this.totalThreadCpuTime = REGISTRY.gaugeLong("thread.total_cpu_ns", this::getTotalThreadCpuTimeNs);
        this.threadStateCount = REGISTRY.gaugeInt("thread.runnable_count", this::getRunnableThreadCount);
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
     * 获取总线程 CPU 时间
     */
    public long getTotalThreadCpuTimeNs() {
        try {
            Long time = (Long) THREAD_BEAN.getClass().getMethod("getTotalThreadCpuTime").invoke(THREAD_BEAN);
            return time != null ? time : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * 获取可运行线程数
     */
    private int getRunnableThreadCount() {
        ThreadInfo[] infos = THREAD_BEAN.dumpAllThreads(false, false);
        int count = 0;
        for (ThreadInfo info : infos) {
            if (info.getThreadState() == Thread.State.RUNNABLE) {
                count++;
            }
        }
        return count;
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

    public int getThreadCount() { return threadCount.getAsInt(); }
    public int getPeakThreadCount() { return peakThreadCount.getAsInt(); }
    public int getDaemonThreadCount() { return daemonThreadCount.getAsInt(); }
    public long getTotalThreadCpuTimeNs() { return totalThreadCpuTime.getAsLong(); }
    public int getRunnableThreadCountGauge() { return threadStateCount.getAsInt(); }
    public Map<String, Long> getThreadStates() { return getThreadStateDistribution(); }

    public record ThreadCpuInfo(
        String name,
        long id,
        String state,
        long cpuTimeNanos
    ) {}
}