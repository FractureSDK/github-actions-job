package dev.vospek.leviathan.observability;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GC 指标收集器
 * <p>
 * 检测当前 GC 实现（ZGC/G1/Shenandoah/Parallel/CMS/Serial），
 * 按 GC 名称分项统计 Count/Time，并提取 JVM GC 启动参数。
 * <p>
 * 对应 Phase 1-A (W1-01) ZGC — GC 观测
 */
public final class GcMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();
    private static final List<GarbageCollectorMXBean> GC_BEANS =
        ManagementFactory.getGarbageCollectorMXBeans();

    private final List<String> gcNames = new ArrayList<>();
    private final List<MetricRegistry.Gauge<Long>> gcCounts = new ArrayList<>();
    private final List<MetricRegistry.Gauge<Long>> gcTimes = new ArrayList<>();
    private final String gcType;
    private final List<String> gcArguments;

    private GcMetrics() {
        for (GarbageCollectorMXBean bean : GC_BEANS) {
            String name = normalizeName(bean.getName());
            gcNames.add(name);
            gcCounts.add(REGISTRY.gaugeLong(
                "gc." + name + ".count", bean::getCollectionCount
            ));
            gcTimes.add(REGISTRY.gaugeLong(
                "gc." + name + ".time_ms", bean::getCollectionTime
            ));
        }
        // GC 实现与 JVM 启动参数在运行期不变，构造时检测一次并缓存，
        // 避免诊断日志周期性重复扫描 MXBean 列表
        this.gcType = detectGcType();
        this.gcArguments = List.copyOf(extractGcArguments());
    }

    private static final class Holder {
        static final GcMetrics INSTANCE = new GcMetrics();
    }

    public static GcMetrics get() {
        return Holder.INSTANCE;
    }

    // ==================== 便捷查询 ====================

    public String getGcType() {
        return gcType;
    }

    public List<String> getGcNames() {
        return gcNames;
    }

    public int getGcBeanCount() {
        return gcNames.size();
    }

    public long getGcCount(int index) {
        return gcCounts.get(index).getAsLong();
    }

    public long getGcTimeMs(int index) {
        return gcTimes.get(index).getAsLong();
    }

    public List<String> getGcParameters() {
        return gcArguments;
    }

    // ==================== 内部检测 ====================

    /**
     * 根据 GC 名称识别当前使用的 GC 实现
     */
    private static String detectGcType() {
        for (GarbageCollectorMXBean bean : GC_BEANS) {
            String name = bean.getName();
            if (name == null) {
                continue;
            }
            String n = name.toLowerCase(Locale.ROOT);
            if (n.contains("zgc") || n.contains("zcollector") || n.contains("zgccycle")) {
                return "ZGC";
            }
            if (n.contains("shenandoah")) {
                return "Shenandoah";
            }
            if (n.contains("g1")) {
                return "G1";
            }
            if (n.contains("parnew") || n.contains("concurrentmarksweep") || n.contains("cms")) {
                return "CMS";
            }
            if (n.startsWith("ps") || n.contains("parallel")) {
                return "Parallel";
            }
            if (n.contains("copy") || n.contains("marksweep")) {
                return "Serial";
            }
        }
        return "Unknown";
    }

    /**
     * 提取启动参数中的 GC 相关 -XX 参数
     */
    private static List<String> extractGcArguments() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        List<String> result = new ArrayList<>();
        for (String arg : runtime.getInputArguments()) {
            if (arg.startsWith("-XX:") && arg.toUpperCase(Locale.ROOT).contains("GC")) {
                result.add(arg);
            }
        }
        return result;
    }

    /**
     * GC 名称归一化为指标 key（小写、非字母数字转点号）
     */
    private static String normalizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append('.');
            }
        }
        return sb.toString();
    }
}