package dev.vospek.leviathan.command.subcommands;

import dev.vospek.leviathan.bootstrap.HardwareCapabilities;
import dev.vospek.leviathan.bootstrap.LeviathanBootstrap;
import dev.vospek.leviathan.bootstrap.RuntimeDetector;
import dev.vospek.leviathan.command.LeviathanCommand;
import dev.vospek.leviathan.command.PermissionedLeviathanSubcommand;
import dev.vospek.leviathan.config.modules.misc.CoreConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.List;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class StatusCommand extends PermissionedLeviathanSubcommand {

    public static final String LITERAL_ARGUMENT = "status";
    public static final String PERM = LeviathanCommand.BASE_PERM + "." + LITERAL_ARGUMENT;
    private static final DecimalFormat DF = new DecimalFormat("########0.0");

    public StatusCommand() {
        super(PERM, PermissionDefault.TRUE);
    }

    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        // Ensure bootstrap is initialized
        LeviathanBootstrap.initialize();

        RuntimeDetector.RuntimeInfo runtime = LeviathanBootstrap.getRuntimeInfo();
        HardwareCapabilities hardware = LeviathanBootstrap.getHardwareCapabilities();

        if (runtime == null || hardware == null) {
            sender.sendMessage(text("Runtime detection not yet initialized", RED));
            return true;
        }

        // Version info
        String leviathanVersion = getLeviathanVersion();
        String leafVersion = getLeafVersion();

        sender.sendMessage(header("Leviathan Status"));

        sender.sendMessage(section("Version"));
        sender.sendMessage(kv("  Leviathan: ", text(leviathanVersion), GRAY, WHITE));
        sender.sendMessage(kv("  Leaf Base:  ", text(leafVersion), GRAY, WHITE));

        sender.sendMessage(empty());
        sender.sendMessage(section("Runtime"));
        sender.sendMessage(kv("  Java:       ", text(runtime.javaVersion + " (" + runtime.vmName + ")"), GRAY, WHITE));
        sender.sendMessage(kv("  OS:         ", text(runtime.osName + " " + runtime.osVersion + " (" + runtime.architecture + ")"), GRAY, WHITE));

        sender.sendMessage(empty());
        sender.sendMessage(section("Hardware"));
        sender.sendMessage(kv("  CPU:        ", text(hardware.logicalProcessors + " logical / " + hardware.physicalProcessors + " physical"), GRAY, WHITE));
        sender.sendMessage(kv("  SIMD/AVX2:  ", text((hardware.hasSIMD ? "SIMD " : "") + (hardware.hasAVX2 ? "AVX2 " : "") + (hardware.hasAVX512 ? "AVX-512" : "")), GRAY, WHITE));
        
        // Memory
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        sender.sendMessage(empty());
        sender.sendMessage(section("Memory"));
        sender.sendMessage(kv("  Heap:       ", text(formatBytes(heapUsage.getUsed()) + " / " + formatBytes(heapUsage.getMax())), GRAY, WHITE));
        sender.sendMessage(kv("  Physical:   ", text(formatBytes(hardware.physicalMemoryBytes)), GRAY, WHITE));
        
        // CPU Load
        sender.sendMessage(empty());
        sender.sendMessage(section("System Load"));
        sender.sendMessage(kv("  CPU Load:   ", text(DF.format(osBean.getSystemLoadAverage())), GRAY, WHITE));
        sender.sendMessage(kv("  Process CPU:", text(getProcessCpuPercent(osBean) + "%"), GRAY, WHITE));

        // Runtime Mode
        sender.sendMessage(empty());
        sender.sendMessage(section("Runtime Mode"));
        sender.sendMessage(kv("  Mode:       ", text(CoreConfig.runtimeMode), GRAY, YELLOW));
        sender.sendMessage(kv("  Safe Mode:  ", text(CoreConfig.isSafeMode() ? "yes" : "no"), GRAY, CoreConfig.isSafeMode() ? RED : GREEN));
        
        // Active Features (from config)
        sender.sendMessage(empty());
        sender.sendMessage(section("Active Features"));
        listActiveFeatures(sender);

        return true;
    }

    private String getLeviathanVersion() {
        try {
            var buildInfo = io.papermc.paper.ServerBuildInfo.buildInfo();
            if (buildInfo.buildNumber().isPresent()) {
                return buildInfo.brandName() + " " + buildInfo.minecraftVersionId() + " (build " + buildInfo.buildNumber().getAsInt() + ")";
            }
            return buildInfo.brandName() + " " + buildInfo.minecraftVersionId() + " (dev)";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getLeafVersion() {
        try {
            return io.papermc.paper.ServerBuildInfo.buildInfo().asString(io.papermc.paper.ServerBuildInfo.StringRepresentation.VERSION_SIMPLE);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private double getProcessCpuPercent(OperatingSystemMXBean osBean) {
        try {
            Double load = (Double) osBean.getClass().getMethod("getProcessCpuLoad").invoke(osBean);
            return load != null ? load * 100.0 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void listActiveFeatures(CommandSender sender) {
        sender.sendMessage(text("  Core Toggles:").color(GRAY));
        sender.sendMessage(featureLine("    Diagnostics:     ", CoreConfig.diagnosticsEnabled));
        sender.sendMessage(featureLine("    Observability:   ", CoreConfig.observabilityEnabled));
        sender.sendMessage(featureLine("    Benchmark:       ", CoreConfig.benchmarkEnabled));
        sender.sendMessage(featureLine("    Experimental:    ", CoreConfig.experimentalEnabled));
        
        sender.sendMessage(text("  Feature Flags:").color(GRAY));
        sender.sendMessage(featureFlagLine("    Linear Storage:  ", CoreConfig.featureLinearStorage));
        sender.sendMessage(featureFlagLine("    Zstd Storage:    ", CoreConfig.featureZstdStorage));
        sender.sendMessage(featureFlagLine("    DAB:             ", CoreConfig.featureDAB));
        sender.sendMessage(featureFlagLine("    Async Chunk:     ", CoreConfig.featureAsyncChunk));
        sender.sendMessage(featureFlagLine("    Region Tick:     ", CoreConfig.featureRegionTick));
        sender.sendMessage(featureFlagLine("    Plugin Async:    ", CoreConfig.featurePluginAsync));
        sender.sendMessage(featureFlagLine("    Hopper Sleep:    ", CoreConfig.featureHopperSleep));
        sender.sendMessage(featureFlagLine("    SIMD:            ", CoreConfig.featureSIMD));
        sender.sendMessage(featureFlagLine("    Zstd Network:    ", CoreConfig.featureZstdNetwork));
        sender.sendMessage(featureFlagLine("    Mmap:            ", CoreConfig.featureMmap));
        sender.sendMessage(featureFlagLine("    RocksDB:         ", CoreConfig.featureRocksDB));
    }

    private static Component featureLine(String label, boolean enabled) {
        return text(label).color(GRAY)
            .append(text(enabled ? "enabled" : "disabled").color(enabled ? GREEN : RED));
    }

    private static Component featureFlagLine(String label, String flag) {
        return text(label).color(GRAY)
            .append(text(flag).color(getFeatureColor(flag)));
    }

    private static NamedTextColor getFeatureColor(String flag) {
        return switch (flag) {
            case "enabled" -> GREEN;
            case "experimental" -> YELLOW;
            case "safe" -> AQUA;
            default -> RED; // disabled
        };
    }

    private static Component header(String title) {
        return text("━━━━━━━━━━━━━ ").color(GOLD)
            .append(text(title).color(YELLOW))
            .append(text(" ━━━━━━━━━━━━━").color(GOLD));
    }

    private static Component section(String title) {
        return text("▸ ").color(AQUA).append(text(title).color(AQUA));
    }

    private static Component kv(String key, Component value, NamedTextColor keyColor, NamedTextColor valueColor) {
        return text(key).color(keyColor).append(value.color(valueColor));
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "unknown";
        if (bytes >= 1024L * 1024 * 1024) return DF.format(bytes / (1024.0 * 1024 * 1024)) + " GB";
        if (bytes >= 1024 * 1024) return DF.format(bytes / (1024.0 * 1024)) + " MB";
        if (bytes >= 1024) return DF.format(bytes / 1024.0) + " KB";
        return bytes + " B";
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String subCommand, final String[] args) {
        return List.of();
    }
}