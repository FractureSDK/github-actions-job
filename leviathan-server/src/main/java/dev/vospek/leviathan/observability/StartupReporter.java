package dev.vospek.leviathan.observability;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

import dev.vospek.leviathan.bootstrap.HardwareCapabilities;
import dev.vospek.leviathan.bootstrap.LeviathanBootstrap;
import dev.vospek.leviathan.bootstrap.RuntimeDetector;
import dev.vospek.leviathan.config.LeviathanConfig;
import dev.vospek.leviathan.config.modules.misc.CoreConfig;
import dev.vospek.leviathan.config.modules.misc.RegionFormatConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Leviathan 启动报告生成器
 * <p>
 * 一次性输出全栈启动摘要（控制台日志），并提供 {@link #buildReport()} 供
 * {@code /leviathan rules} 命令复用。汇总领域：
 * JVM / Storage / Network / Entity / Async / Configuration / Feature / Hardware。
 * <p>
 * 对应 Phase 1-G (W7-03) Final Runtime Report
 */
public final class StartupReporter {

    private StartupReporter() {
    }

    /**
     * 启动时向控制台输出报告（由 {@link ObservabilityBootstrap#initialize()} 调用）
     */
    public static void report() {
        LeviathanConfig.LOGGER.info("━━━━━━━━━━━━━ Leviathan Startup Report ━━━━━━━━━━━━━");
        LeviathanConfig.LOGGER.info("{}", buildReport().replaceAll("\n", "\n  "));
        LeviathanConfig.LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 构造扁平化纯文本报告（用于日志）
     */
    public static String buildReport() {
        StringBuilder sb = new StringBuilder(1024);

        // JVM
        RuntimeDetector.RuntimeInfo rt = LeviathanBootstrap.getRuntimeInfo();
        if (rt != null) {
            sb.append("JVM: ").append(rt.javaVersion).append(" / ").append(rt.vmName)
                .append(" / OS ").append(rt.osName).append(' ')
                .append(rt.architecture).append('\n');
            sb.append("JVM Args: ").append(rt.inputArguments != null
                ? String.join(" ", rt.inputArguments) : "n/a").append('\n');
        }

        // Storage
        sb.append("Storage: region=").append(RegionFormatConfig.regionFormatName)
            .append(", compression=").append(RegionFormatConfig.compressionLevel)
            .append(", io threads=").append(RegionFormatConfig.ioThreadCount)
            .append(", flush delay=").append(RegionFormatConfig.ioFlushDelay).append("ms")
            .append(", virtual thread=")
            .append(RegionFormatConfig.linearUseVirtualThread).append('\n');

        // Network
        NetworkMetrics nm = NetworkMetrics.get();
        sb.append("Network: zstd=").append(nm.isZstdEnabled() ? "on" : "off")
            .append(", compression threshold=").append(nm.getCompressionThreshold())
            .append(", connections=").append(nm.getConnectionCount()).append('\n');

        // Entity
        EntityMetrics em = EntityMetrics.get();
        sb.append("Entity: active=").append(em.getActiveEntityCount())
            .append(", ticking=").append(em.getTickingEntityCount())
            .append(", DAB shadow reduced ticks=")
            .append(em.getDabShadowReducedTicks()).append('\n');

        // Async
        AsyncMetrics am = AsyncMetrics.get();
        sb.append("Async: save queue=").append(am.getSaveQueueDepth())
            .append(", load queue=").append(am.getLoadQueueDepth())
            .append(", scheduler sync/async/deferred=")
            .append(am.getSyncTaskCount()).append('/')
            .append(am.getAsyncTaskCount()).append('/')
            .append(am.getDeferredTaskCount()).append('\n');

        // Configuration
        sb.append("Config: runtime.mode=").append(CoreConfig.runtimeMode)
            .append(", diagnostics=").append(CoreConfig.diagnosticsEnabled)
            .append(", observability=").append(CoreConfig.observabilityEnabled)
            .append(", benchmark=").append(CoreConfig.benchmarkEnabled)
            .append(", experimental=").append(CoreConfig.experimentalEnabled).append('\n');

        // Features
        sb.append("Features: linear=").append(CoreConfig.featureLinearStorage)
            .append(", zstd-storage=").append(CoreConfig.featureZstdStorage)
            .append(", dab=").append(CoreConfig.featureDAB)
            .append(", async-chunk=").append(CoreConfig.featureAsyncChunk)
            .append(", region-tick=").append(CoreConfig.featureRegionTick)
            .append(", plugin-async=").append(CoreConfig.featurePluginAsync)
            .append(", hopper-sleep=").append(CoreConfig.featureHopperSleep)
            .append(", simd=").append(CoreConfig.featureSIMD)
            .append(", zstd-network=").append(CoreConfig.featureZstdNetwork)
            .append(", mmap=").append(CoreConfig.featureMmap)
            .append(", rocksdb=").append(CoreConfig.featureRocksDB).append('\n');

        // Benchmark status
        sb.append("Benchmark framework: ")
            .append(CoreConfig.benchmarkEnabled ? "enabled" : "disabled").append('\n');

        // Hardware
        HardwareCapabilities hw = LeviathanBootstrap.getHardwareCapabilities();
        if (hw != null) {
            sb.append("Hardware: ")
                .append(hw.physicalProcessors).append('/').append(hw.logicalProcessors)
                .append(" CPUs, SIMD=").append(hw.hasSIMD).append(", AVX2=").append(hw.hasAVX2)
                .append(", AVX-512=").append(hw.hasAVX512).append('\n');
        }

        return sb.toString().trim();
    }

    /**
     * 构造组件化报告（用于 {@code /leviathan rules} 命令输出 / Compat 报告）
     */
    public static Component buildComponentReport() {
        StringBuilder sb = new StringBuilder();
        RuntimeDetector.RuntimeInfo rt = LeviathanBootstrap.getRuntimeInfo();
        if (rt != null) {
            sb.append("JVM: ").append(rt.javaVersion).append(" / ").append(rt.vmName).append('\n');
            sb.append("OS:  ").append(rt.osName).append(' ').append(rt.architecture).append('\n');
        }

        sb.append("Storage: region=").append(RegionFormatConfig.regionFormatName)
            .append(" (compression ").append(RegionFormatConfig.compressionLevel)
            .append(", io threads ").append(RegionFormatConfig.ioThreadCount).append(")\n");

        sb.append("Network: zstd=").append(NetworkMetrics.get().isZstdEnabled() ? "on" : "off")
            .append("\n");

        sb.append("Entity: active=").append(EntityMetrics.get().getActiveEntityCount())
            .append(", ticking=").append(EntityMetrics.get().getTickingEntityCount()).append("\n");

        AsyncMetrics am = AsyncMetrics.get();
        sb.append("Async: save queue=").append(am.getSaveQueueDepth())
            .append(", load queue=").append(am.getLoadQueueDepth()).append("\n");

        return text("Leviathan Startup Report").color(AQUA)
            .append(text("\n" + sb).color(GRAY));
    }

    /**
     * 构造有效 Feature 状态列表组件（用于 {@code /leviathan rules} 命令核心段）
     * <p>
     * 与 {@code /leviathan status} 的扁平列表不同：这里按域分组并显示
     * <b>有效状态</b>（safe 模式将 experimental 降级为 disabled）。
     */
    public static Component buildEffectiveFeatures() {
        boolean safe = CoreConfig.isSafeMode();

        Component component = text("━━━━━━━ Leviathan Rules ━━━━━━━").color(GOLD).append(empty());

        component = component.append(empty())
            .append(text("▸ Storage").color(AQUA)).append(empty())
            .append(featureRow("  Linear Storage ", CoreConfig.featureLinearStorage, safe))
            .append(featureRow("  Zstd Storage   ", CoreConfig.featureZstdStorage, safe))
            .append(featureRow("  Mmap I/O       ", CoreConfig.featureMmap, safe))
            .append(featureRow("  RocksDB        ", CoreConfig.featureRocksDB, safe));

        component = component.append(empty())
            .append(text("▸ Network").color(AQUA)).append(empty())
            .append(featureRow("  Zstd Network   ", CoreConfig.featureZstdNetwork, safe))
            .append(featureRow("  Async Chunk    ", CoreConfig.featureAsyncChunk, safe));

        component = component.append(empty())
            .append(text("▸ Entity / Simulation").color(AQUA)).append(empty())
            .append(featureRow("  DAB            ", CoreConfig.featureDAB, safe))
            .append(featureRow("  Hopper Sleep   ", CoreConfig.featureHopperSleep, safe))
            .append(featureRow("  SIMD           ", CoreConfig.featureSIMD, safe));

        component = component.append(empty())
            .append(text("▸ Scheduling").color(AQUA)).append(empty())
            .append(featureRow("  Region Tick    ", CoreConfig.featureRegionTick, safe))
            .append(featureRow("  Plugin Async   ", CoreConfig.featurePluginAsync, safe));

        component = component.append(empty())
            .append(text("▸ Core").color(AQUA)).append(empty())
            .append(toggleRow("  Diagnostics    ", CoreConfig.diagnosticsEnabled))
            .append(toggleRow("  Observability  ", CoreConfig.observabilityEnabled))
            .append(toggleRow("  Benchmark      ", CoreConfig.benchmarkEnabled))
            .append(toggleRow("  Experimental   ", CoreConfig.experimentalEnabled))
            .append(text("  Runtime Mode   ").color(GRAY)
                .append(text(CoreConfig.runtimeMode).color(YELLOW)).append(empty()));

        if (safe) {
            component = component.append(empty())
                .append(text("Safe mode: experimental flags downgraded to disabled.")
                    .color(YELLOW));
        }

        return component;
    }

    private static Component featureRow(String label, String flag, boolean safeMode) {
        String effective = effectiveFlag(flag, safeMode);
        return text(label).color(GRAY)
            .append(text(effective).color(colorForFlag(effective)))
            .append(text("  (").color(GRAY))
            .append(text("config: " + flag).color(GRAY))
            .append(text(")").color(GRAY))
            .append(empty());
    }

    private static Component toggleRow(String label, boolean enabled) {
        return text(label).color(GRAY)
            .append(text(enabled ? "enabled" : "disabled").color(enabled ? GREEN : RED))
            .append(empty());
    }

    /**
     * 计算有效 flag 状态：safe 模式将 experimental 降级为 disabled
     */
    private static String effectiveFlag(String flag, boolean safeMode) {
        if (safeMode && "experimental".equals(flag)) {
            return "disabled";
        }
        return flag;
    }

    private static NamedTextColor colorForFlag(String flag) {
        return switch (flag) {
            case "enabled" -> GREEN;
            case "experimental" -> YELLOW;
            case "safe" -> AQUA;
            default -> RED; // disabled
        };
    }
}