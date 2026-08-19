package dev.vospek.leviathan.observability;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 结构化诊断日志系统
 * <p>
 * 将高频性能统计、诊断信息写入 {@code logs/leviathan/*.log} 文件，
 * 支持结构化字段（JSON 格式），便于日志聚合和分析。
 * <p>
 * 对应 Phase 0-D: P0-010
 */
public final class DiagnosticsLogger {

    private static final String LOG_DIR = "logs/leviathan";
    private static final Map<String, FileAppender> appenders = new ConcurrentHashMap<>();

    private DiagnosticsLogger() {
    }

    /**
     * 初始化诊断日志系统
     */
    public static synchronized void initialize() {
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // 创建各分类的文件 Appender
        String[] categories = {
            "runtime", "performance", "network", "storage",
            "entity", "diagnostics", "benchmark"
        };

        for (String category : categories) {
            getAppender(category); // 懒加载创建
        }
    }

    /**
     * 获取或创建分类 Appender
     */
    private static FileAppender getAppender(String category) {
        return appenders.computeIfAbsent(category, cat -> {
            try {
                return new FileAppender(category);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create diagnostics appender for " + category, e);
            }
        });
    }

    /**
     * 写入运行时诊断信息
     */
    public static void logRuntime(String message, Object... args) {
        getAppender("runtime").write(format(message, args));
    }

    /**
     * 写入性能统计信息
     */
    public static void logPerformance(String message, Object... args) {
        getAppender("performance").write(format(message, args));
    }

    /**
     * 写入网络诊断信息
     */
    public static void logNetwork(String message, Object... args) {
        getAppender("network").write(format(message, args));
    }

    /**
     * 写入存储诊断信息
     */
    public static void logStorage(String message, Object... args) {
        getAppender("storage").write(format(message, args));
    }

    /**
     * 写入实体诊断信息
     */
    public static void logEntity(String message, Object... args) {
        getAppender("entity").write(format(message, args));
    }

    /**
     * 写入通用诊断信息
     */
    public static void logDiagnostics(String message, Object... args) {
        getAppender("diagnostics").write(format(message, args));
    }

    /**
     * 写入基准测试信息
     */
    public static void logBenchmark(String message, Object... args) {
        getAppender("benchmark").write(format(message, args));
    }

    /**
     * 写入结构化字段日志（JSON 格式）
     */
    public static void logStructured(String category, Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append(Instant.now().toString()).append(' ');
        sb.append(category).append(' ');

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append(' ');
        }

        getAppender(category).write(sb.toString().trim());
    }

    /**
     * 记录 Tick 性能数据
     * <p>
     * 时间参数均为毫秒，来自同步的 tickTimes 窗口（avg/min/max）。
     * overruns/spikes 来自计数器，在 tick 钩子接入前为 0。
     */
    public static void logTickMetrics(double tps, double avgMs, double minMs, double maxMs, long overruns, long spikes) {
        logStructured("performance", Map.of(
            "type", "tick_metrics",
            "tps", String.format("%.2f", tps),
            "mspt_avg_ms", String.format("%.2f", avgMs),
            "mspt_min_ms", String.format("%.2f", minMs),
            "mspt_max_ms", String.format("%.2f", maxMs),
            "overruns", overruns,
            "spikes", spikes
        ));
    }

    /**
     * 记录内存指标
     */
    public static void logMemoryMetrics(long heapUsed, long heapMax, long directUsed, double allocationRate) {
        logStructured("performance", Map.of(
            "type", "memory_metrics",
            "heap_used_mb", heapUsed / 1024 / 1024,
            "heap_max_mb", heapMax / 1024 / 1024,
            "heap_usage_pct", String.format("%.1f", heapUsed * 100.0 / heapMax),
            "direct_mb", directUsed / 1024 / 1024,
            "alloc_rate_mb_s", String.format("%.2f", allocationRate)
        ));
    }

    /**
     * 记录 GC 事件
     */
    public static void logGcEvent(String gcName, long durationMs, long heapBefore, long heapAfter) {
        logStructured("performance", Map.of(
            "type", "gc_event",
            "gc_name", gcName,
            "duration_ms", durationMs,
            "heap_before_mb", heapBefore / 1024 / 1024,
            "heap_after_mb", heapAfter / 1024 / 1024,
            "freed_mb", (heapBefore - heapAfter) / 1024 / 1024
        ));
    }

    private static String format(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }

    /**
     * 文件 Appender - 直接写入文件，避免 Log4j 开销
     */
    private static final class FileAppender {
        private final Path filePath;
        private final java.io.BufferedWriter writer;

        private FileAppender(String category) throws IOException {
            this.filePath = Path.of(LOG_DIR, category + ".log");
            // 确保父目录存在
            Files.createDirectories(filePath.getParent());
            // 追加模式打开
            this.writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        void write(String line) {
            // BufferedWriter 非线程安全，metrics 同步线程与命令线程可能并发写入
            synchronized (this) {
                try {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    // 忽略写入错误，避免影响主线程
                }
            }
        }

        void close() {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
    }
}