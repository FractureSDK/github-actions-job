package dev.vospek.leviathan.observability;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Leviathan 统一日志系统
 * <p>
 * 建立统一的日志命名空间：{@code [Leviathan/...]}，所有模块通过此类获取 Logger。
 * 高频性能统计写入 {@code logs/leviathan/*.log} 而非控制台。
 * <p>
 * 对应 Phase 0-D: P0-009, P0-010
 */
public final class LeviathanLogger {

    private static final String LOG_DIR = "logs/leviathan";
    private static final String NAMESPACE_PREFIX = "Leviathan";

    private static final Map<String, Logger> loggerCache = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private LeviathanLogger() {
    }

    /**
     * 初始化日志系统 - 创建目录、配置文件 Appender
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        // 创建日志目录
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // 配置文件 Appender 到根 Logger
        configureFileAppenders();

        initialized = true;
    }

    /**
     * 获取模块 Logger - 命名空间自动添加 Leviathan 前缀
     * <p>
     * 示例：{@code LeviathanLogger.getLogger("Storage")} -> {@code Logger[Leviathan/Storage]}
     */
    public static Logger getLogger(String module) {
        String name = NAMESPACE_PREFIX + "/" + module;
        return loggerCache.computeIfAbsent(name, LogManager::getLogger);
    }

    /**
     * 获取子模块 Logger
     * <p>
     * 示例：{@code LeviathanLogger.getLogger("Storage", "Region")} -> {@code Logger[Leviathan/Storage/Region]}
     */
    public static Logger getLogger(String module, String subModule) {
        String name = NAMESPACE_PREFIX + "/" + module + "/" + subModule;
        return loggerCache.computeIfAbsent(name, LogManager::getLogger);
    }

    /**
     * 获取性能统计专用 Logger - 写入文件不输出控制台
     */
    public static Logger getPerformanceLogger(String category) {
        String name = NAMESPACE_PREFIX + "/Performance/" + category;
        return loggerCache.computeIfAbsent(name, n -> {
            Logger logger = LogManager.getLogger(n);
            // 性能日志器默认不继承根 Logger 的控制台 Appender
            if (logger instanceof org.apache.logging.log4j.core.Logger coreLogger) {
                coreLogger.setAdditive(false);
            }
            return logger;
        });
    }

    /**
     * 获取诊断 Logger - 写入 diagnostics.log
     */
    public static Logger getDiagnosticsLogger() {
        return getPerformanceLogger("Diagnostics");
    }

    /**
     * 获取网络 Logger - 写入 network.log
     */
    public static Logger getNetworkLogger() {
        return getPerformanceLogger("Network");
    }

    /**
     * 获取存储 Logger - 写入 storage.log
     */
    public static Logger getStorageLogger() {
        return getPerformanceLogger("Storage");
    }

    /**
     * 获取实体 Logger - 写入 entity.log
     */
    public static Logger getEntityLogger() {
        return getPerformanceLogger("Entity");
    }

    /**
     * 获取区域 Logger - 写入 region.log
     */
    public static Logger getRegionLogger() {
        return getPerformanceLogger("Region");
    }

    /**
     * 获取基准测试 Logger - 写入 benchmark.log
     */
    public static Logger getBenchmarkLogger() {
        return getPerformanceLogger("Benchmark");
    }

    /**
     * 获取运行时 Logger - 写入 runtime.log
     */
    public static Logger getRuntimeLogger() {
        return getPerformanceLogger("Runtime");
    }

    private static void configureFileAppenders() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();

            PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                .withConfiguration(config)
                .build();

            // 为每个性能分类创建 RollingFileAppender
            String[] categories = {"Diagnostics", "Network", "Storage", "Entity", "Region", "Benchmark", "Runtime", "Performance"};

            for (String category : categories) {
                String loggerName = NAMESPACE_PREFIX + "/Performance/" + category;
                String fileName = LOG_DIR + "/" + category.toLowerCase() + ".log";

                RollingFileAppender appender = RollingFileAppender.newBuilder()
                    .setName("Leviathan-" + category)
                    .withFileName(fileName)
                    .withFilePattern(fileName + ".%i.gz")
                    .setLayout(layout)
                    .withPolicy(SizeBasedTriggeringPolicy.createPolicy("10MB"))
                    .withStrategy(DefaultRolloverStrategy.createStrategy("5", "1", "5", "true", null, false, config))
                    .setConfiguration(config)
                    .build();

                appender.start();
                config.addAppender(appender);

                org.apache.logging.log4j.core.Logger coreLogger = ctx.getLogger(loggerName);
                coreLogger.addAppender(appender);
                coreLogger.setLevel(Level.INFO);
                coreLogger.setAdditive(false); // 不传播到根 Logger（控制台）
            }

            ctx.updateLoggers();
        } catch (Exception e) {
            // 如果 Log4j2 核心 API 不可用，回退到基础配置
            System.err.println("[LeviathanLogger] Failed to configure file appenders: " + e.getMessage());
        }
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }
}