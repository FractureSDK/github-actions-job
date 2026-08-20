package dev.vospek.leviathan.observability;

import dev.vospek.leviathan.config.LeviathanConfig;
import dev.vospek.leviathan.config.modules.misc.CoreConfig;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;

/**
 * 可观测性系统启动类
 * <p>
 * 初始化指标收集、定期同步、诊断日志等。
 * <p>
 * 对应 Phase 0-E: P0-011 ~ P0-015
 */
public final class ObservabilityBootstrap {

    private static volatile boolean initialized = false;
    private static ScheduledExecutorService scheduler;

    private ObservabilityBootstrap() {
    }

    /**
     * 初始化可观测性系统
     * <p>
     * 应在配置加载完成后调用
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        // 检查配置是否启用
        if (!CoreConfig.observabilityEnabled) {
            LeviathanConfig.LOGGER.info("Observability system disabled via config");
            return;
        }

        // 初始化日志系统
        LeviathanLogger.initialize();
        DiagnosticsLogger.initialize();

        // 初始化指标收集器（触发懒加载）
        MetricRegistry.get();
        TickMetrics.get();
        CpuMetrics.get();
        MemoryMetrics.get();
        ThreadMetrics.get();

        // 启动定期同步任务
        startPeriodicSync();

        // 注册 JVM 关闭钩子作为兜底
        Runtime.getRuntime().addShutdownHook(
            new Thread(ObservabilityBootstrap::shutdown, "Leviathan-Observability-Shutdown")
        );

        initialized = true;
        LeviathanConfig.LOGGER.info("Leviathan Observability system initialized");
    }

    /**
     * 启动定期指标同步任务
     */
    private static void startPeriodicSync() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Leviathan-Metrics-Sync");
            t.setDaemon(true);
            return t;
        });

        // 每 5 秒同步一次指标（与 tickTimes5s 窗口对齐）
        scheduler.scheduleAtFixedRate(() -> {
            try {
                syncMetrics();
            } catch (Exception e) {
                LeviathanConfig.LOGGER.warn("Failed to sync metrics: {}", e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);

        // 每分钟记录一次诊断日志
        scheduler.scheduleAtFixedRate(() -> {
            try {
                logDiagnostics();
            } catch (Exception e) {
                LeviathanConfig.LOGGER.warn("Failed to log diagnostics: {}", e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 从服务器同步指标
     */
    private static void syncMetrics() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }

        // 同步 Tick 指标
        TickMetrics tm = TickMetrics.get();
        tm.syncFromServer(server);

        // 记录诊断日志
        DiagnosticsLogger.logTickMetrics(
            tm.getSyncedTPS(),
            tm.getSyncedMSPT(),
            tm.getSyncedMinMSPT(),
            tm.getSyncedMaxMSPT(),
            tm.getOverrunCount(),
            tm.getSpikeCount()
        );

        // 记录内存指标
        MemoryMetrics mm = MemoryMetrics.get();
        DiagnosticsLogger.logMemoryMetrics(
            mm.getHeapUsed(),
            mm.getHeapMax(),
            mm.getDirectMemoryUsed(),
            mm.getAllocationRateMbPerSec()
        );
    }

    /**
     * 记录完整诊断快照
     */
    private static void logDiagnostics() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }

        // 线程信息
        ThreadMetrics thm = ThreadMetrics.get();
        DiagnosticsLogger.logDiagnostics("Thread count: %d, Peak: %d, Daemon: %d, CPU: %.2fs",
            thm.getThreadCount(), thm.getPeakThreadCount(), thm.getDaemonThreadCount(),
            thm.getTotalThreadCpuTimeNs() / 1_000_000_000.0);

        // CPU 信息
        CpuMetrics cm = CpuMetrics.get();
        DiagnosticsLogger.logDiagnostics("CPU Process: %.1f%%, System Load: %.2f, Cores: %d",
            cm.getProcessCpuPercent(), cm.getSystemLoadAverage(), cm.getAvailableProcessors());

        // GC 信息
        MemoryMetrics mm = MemoryMetrics.get();
        DiagnosticsLogger.logDiagnostics("GC Count: %d, GC Time: %dms, Allocation Rate: %.2fMB/s",
            mm.getGcCount(), mm.getGcTime(), mm.getAllocationRateMbPerSec());
    }

    /**
     * 关闭可观测性系统
     */
    public static synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        initialized = false;
    }

    /**
     * 检查可观测性系统是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }
}