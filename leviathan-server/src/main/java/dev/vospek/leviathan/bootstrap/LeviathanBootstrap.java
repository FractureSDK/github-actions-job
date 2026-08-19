package dev.vospek.leviathan.bootstrap;

import dev.vospek.leviathan.config.LeviathanConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Leviathan 统一启动入口
 * <p>
 * 在服务器启动早期初始化：
 * <ul>
 *   <li>运行时基线检测 (P0-004)</li>
 *   <li>硬件能力探测 (P0-005)</li>
 * </ul>
 * 后续可扩展：配置系统初始化、Feature Flag 注册、诊断系统初始化等
 */
public final class LeviathanBootstrap {

    private static final Logger LOGGER = LogManager.getLogger(LeviathanBootstrap.class);

    private static RuntimeDetector.RuntimeInfo runtimeInfo;
    private static HardwareCapabilities hardwareCapabilities;
    private static boolean initialized = false;

    private LeviathanBootstrap() {
    }

    /**
     * 初始化 Leviathan 基础设施
     * <p>
     * 应在服务器启动尽早调用（LeviathanConfig.loadConfig() 之前或同时）
     */
    public static synchronized void initialize() {
        if (initialized) {
            LOGGER.warn("LeviathanBootstrap already initialized, skipping");
            return;
        }

        long startTime = System.nanoTime();
        LOGGER.info("Initializing Leviathan Bootstrap...");

        // 1. 运行时基线检测 (P0-004)
        runtimeInfo = RuntimeDetector.detect();

        // 2. 硬件能力探测 (P0-005)
        hardwareCapabilities = HardwareCapabilities.detect();

        // 关联硬件能力到运行时信息
        runtimeInfo.hardware = hardwareCapabilities;

        initialized = true;

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        LOGGER.info("Leviathan Bootstrap initialized in {}ms", elapsed);
    }

    /**
     * 获取运行时基线信息
     */
    public static RuntimeDetector.RuntimeInfo getRuntimeInfo() {
        if (!initialized) {
            initialize();
        }
        return runtimeInfo;
    }

    /**
     * 获取硬件能力
     */
    public static HardwareCapabilities getHardwareCapabilities() {
        if (!initialized) {
            initialize();
        }
        return hardwareCapabilities;
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 重置状态（仅用于测试）
     */
    public static synchronized void resetForTesting() {
        runtimeInfo = null;
        hardwareCapabilities = null;
        initialized = false;
        HardwareCapabilities.Capabilities.resetForTesting();
    }
}