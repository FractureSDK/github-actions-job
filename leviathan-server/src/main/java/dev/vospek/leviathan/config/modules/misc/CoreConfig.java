package dev.vospek.leviathan.config.modules.misc;

import dev.vospek.leviathan.bootstrap.HardwareCapabilities;
import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.LeviathanConfig;
import dev.vospek.leviathan.config.LeviathanGlobalConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Leviathan 核心配置模块
 * <p>
 * 包含运行时模式、诊断、可观测性、基准测试、实验性功能等核心开关
 * 对应 Phase 0-C: P0-006, P0-007, P0-008
 */
public class CoreConfig extends ConfigModule {

    public String basePath() {
        return "leviathan";
    }

    // Runtime mode
    public static String runtimeMode = "normal"; // normal, safe

    // Core feature toggles
    public static boolean diagnosticsEnabled = true;
    public static boolean observabilityEnabled = true;
    public static boolean benchmarkEnabled = false;
    public static boolean experimentalEnabled = false;

    // Feature flags (disabled, safe, enabled, experimental)
    public static String featureLinearStorage = "disabled";
    public static String featureZstdStorage = "disabled";
    public static String featureDAB = "disabled";
    public static String featureAsyncChunk = "disabled";
    public static String featureRegionTick = "disabled";
    public static String featurePluginAsync = "disabled";
    public static String featureHopperSleep = "disabled";
    public static String featureSIMD = "disabled";
    public static String featureZstdNetwork = "disabled";
    public static String featureMmap = "disabled";
    public static String featureRocksDB = "disabled";

    @Override
    public void onLoaded() {
        LeviathanGlobalConfig global = globalConfig;

        // Runtime mode
        runtimeMode = global.getString(basePath() + ".runtime.mode", runtimeMode,
            global.pickStringRegionBased(
                "Runtime mode: normal, safe (safe mode disables experimental features)",
                "运行时模式: normal, safe (安全模式禁用实验性功能)"));

        // Core toggles
        diagnosticsEnabled = global.getBoolean(basePath() + ".diagnostics.enabled", diagnosticsEnabled,
            global.pickStringRegionBased(
                "Enable diagnostics system",
                "启用诊断系统"));

        observabilityEnabled = global.getBoolean(basePath() + ".observability.enabled", observabilityEnabled,
            global.pickStringRegionBased(
                "Enable observability/metrics system",
                "启用可观测性/指标系统"));

        benchmarkEnabled = global.getBoolean(basePath() + ".benchmark.enabled", benchmarkEnabled,
            global.pickStringRegionBased(
                "Enable benchmark framework",
                "启用基准测试框架"));

        experimentalEnabled = global.getBoolean(basePath() + ".experimental.enabled", experimentalEnabled,
            global.pickStringRegionBased(
                "Enable experimental features globally",
                "全局启用实验性功能"));

        // Feature flags - 四态: disabled, safe, enabled, experimental
        featureLinearStorage = global.getString(basePath() + ".features.linear-storage", featureLinearStorage,
            global.pickStringRegionBased(
                "Linear storage feature flag: disabled, safe, enabled, experimental",
                "线性存储功能标志: disabled, safe, enabled, experimental"));

        featureZstdStorage = global.getString(basePath() + ".features.zstd-storage", featureZstdStorage,
            global.pickStringRegionBased(
                "Zstd storage feature flag: disabled, safe, enabled, experimental",
                "Zstd 存储功能标志: disabled, safe, enabled, experimental"));

        featureDAB = global.getString(basePath() + ".features.dab", featureDAB,
            global.pickStringRegionBased(
                "DAB (Delta Area Block) feature flag: disabled, safe, enabled, experimental",
                "DAB 功能标志: disabled, safe, enabled, experimental"));

        featureAsyncChunk = global.getString(basePath() + ".features.async-chunk", featureAsyncChunk,
            global.pickStringRegionBased(
                "Async chunk feature flag: disabled, safe, enabled, experimental",
                "异步区块功能标志: disabled, safe, enabled, experimental"));

        featureRegionTick = global.getString(basePath() + ".features.region-tick", featureRegionTick,
            global.pickStringRegionBased(
                "Region tick feature flag: disabled, safe, enabled, experimental",
                "区域 Tick 功能标志: disabled, safe, enabled, experimental"));

        featurePluginAsync = global.getString(basePath() + ".features.plugin-async", featurePluginAsync,
            global.pickStringRegionBased(
                "Plugin async feature flag: disabled, safe, enabled, experimental",
                "插件异步功能标志: disabled, safe, enabled, experimental"));

        featureHopperSleep = global.getString(basePath() + ".features.hopper-sleep", featureHopperSleep,
            global.pickStringRegionBased(
                "Hopper sleep feature flag: disabled, safe, enabled, experimental",
                "漏斗休眠功能标志: disabled, safe, enabled, experimental"));

        featureSIMD = global.getString(basePath() + ".features.simd", featureSIMD,
            global.pickStringRegionBased(
                "SIMD optimization feature flag: disabled, safe, enabled, experimental",
                "SIMD 优化功能标志: disabled, safe, enabled, experimental"));

        featureZstdNetwork = global.getString(basePath() + ".features.zstd-network", featureZstdNetwork,
            global.pickStringRegionBased(
                "Zstd network compression feature flag: disabled, safe, enabled, experimental",
                "Zstd 网络压缩功能标志: disabled, safe, enabled, experimental"));

        featureMmap = global.getString(basePath() + ".features.mmap", featureMmap,
            global.pickStringRegionBased(
                "Memory-mapped I/O feature flag: disabled, safe, enabled, experimental",
                "内存映射 I/O 功能标志: disabled, safe, enabled, experimental"));

        featureRocksDB = global.getString(basePath() + ".features.rocksdb", featureRocksDB,
            global.pickStringRegionBased(
                "RocksDB storage feature flag: disabled, safe, enabled, experimental",
                "RocksDB 存储功能标志: disabled, safe, enabled, experimental"));
    }

    /**
     * 验证特性标志值是否合法
     */
    public static boolean isValidFeatureFlag(String value) {
        return "disabled".equals(value) || "safe".equals(value) || "enabled".equals(value) || "experimental".equals(value);
    }

    /**
     * 检查功能是否启用（enabled 或 experimental）
     */
    public static boolean isFeatureEnabled(String flag) {
        return "enabled".equals(flag) || "experimental".equals(flag);
    }

    /**
     * 检查功能是否处于实验性模式
     */
    public static boolean isFeatureExperimental(String flag) {
        return "experimental".equals(flag);
    }

    /**
     * 检查是否为安全模式
     */
    public static boolean isSafeMode() {
        return "safe".equalsIgnoreCase(runtimeMode);
    }

    /**
     * 配置校验 - 在所有模块加载后调用
     * 对应 Phase 0-C: P0-008
     */
    public static void validateConfig() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Validate runtime mode
        if (!"normal".equals(runtimeMode) && !"safe".equals(runtimeMode)) {
            errors.add("leviathan.runtime.mode must be 'normal' or 'safe', got: " + runtimeMode);
        }

        // Validate feature flags
        String[] featureFlags = {
            "linear-storage", featureLinearStorage,
            "zstd-storage", featureZstdStorage,
            "dab", featureDAB,
            "async-chunk", featureAsyncChunk,
            "region-tick", featureRegionTick,
            "plugin-async", featurePluginAsync,
            "hopper-sleep", featureHopperSleep,
            "simd", featureSIMD,
            "zstd-network", featureZstdNetwork,
            "mmap", featureMmap,
            "rocksdb", featureRocksDB
        };

        for (int i = 0; i < featureFlags.length; i += 2) {
            String name = featureFlags[i];
            String value = featureFlags[i + 1];
            if (!isValidFeatureFlag(value)) {
                errors.add("leviathan.features." + name + " must be one of: disabled, safe, enabled, experimental, got: " + value);
            }
        }

        // Check for conflicting configurations
        if (isSafeMode()) {
            // In safe mode, warn about experimental features being enabled
            for (int i = 0; i < featureFlags.length; i += 2) {
                String name = featureFlags[i];
                String value = featureFlags[i + 1];
                if ("experimental".equals(value)) {
                    warnings.add("Safe mode enabled but leviathan.features." + name + " is experimental; will be treated as disabled");
                }
            }
        }

        // Check hardware capability requirements
        HardwareCapabilities caps = HardwareCapabilities.Capabilities.get();
        if (caps != null) {
            if (isFeatureEnabled(featureSIMD) && !caps.hasSIMD) {
                warnings.add("SIMD feature enabled but hardware does not support SIMD");
            }
            if (isFeatureEnabled(featureSIMD) && !caps.hasAVX2) {
                warnings.add("SIMD feature enabled but hardware does not support AVX2");
            }
        }

        // Log results
        if (!errors.isEmpty()) {
            for (String error : errors) {
                LeviathanConfig.LOGGER.error("[Config Validation] " + error);
            }
            throw new IllegalStateException("Leviathan configuration validation failed: " + errors.size() + " error(s)");
        }

        if (!warnings.isEmpty()) {
            for (String warning : warnings) {
                LeviathanConfig.LOGGER.warn("[Config Validation] " + warning);
            }
        }

        if (errors.isEmpty() && warnings.isEmpty()) {
            LeviathanConfig.LOGGER.info("Leviathan configuration validation passed");
        }
    }

    @Override
    public void onPostLoaded() {
        validateConfig();
    }
}