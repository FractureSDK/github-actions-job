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

        sender.sendMessage(text("━━━━━━━━━━━━━ ").color(GOLD)
            .append(text("Leviathan Status").color(YELLOW))
            .append(text(" ━━━━━━━━━━━━━").color(GOLD))
            .build());

        sender.sendMessage(text(""));
        sender.sendMessage(text("▸ Version").color(AQUA));
        sender.sendMessage(text("  Leviathan: ").color(GRAY).append(text(leviathanVersion).color(WHITE)));
        sender.sendMessage(text("  Leaf Base:  ").color(GRAY).append(text(leafVersion).color(WHITE)));

        sender.sendMessage(text(""));
        sender.sendMessage(text("▸ Runtime").color(AQUA));
        sender.sendMessage(text("  Java:       ").color(GRAY).append(text(runtime.javaVersion + " (" + runtime.vmName + ")").color(WHITE)));
        sender.sendMessage(text("  OS:         ").color(GRAY).append(text(runtime.osName + " " + runtime.osVersion + " (" + runtime.architecture + ")").color(WHITE)));

        sender.sendMessage(text(""));
        sender.sendMessage(text("▸ Hardware").color(AQUA));
        sender.sendMessage(text("  CPU:        ").color(GRAY).append(text(hardware.logicalProcessors + " logical / " + hardware.physicalProcessors + " physical").color(WHITE)));
        sender.sendMessage(text("  SIMD/AVX2:  ").color(GRAY).append(text((hardware.hasSIMD ? "SIMD " : "") + (hardware.hasAVX2 ? "AVX2 " : "") + (hardware.hasAVX512 ? "AVX-512" : "")).color(WHITE)));
        
        // Memory
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        sender.sendMessage(text(""));
        sender.sendMessage(text("▸ Memory").color(AQUA));
        sender.sendMessage(text("  Heap:       ").color(GRAY).append(text(formatBytes(heapUsage.getUsed()) + " / " + formatBytes(heapUsage.getMax())).color(WHITE)));
        sender.sendMessage(text("  Physical:   ").color(GRAY).append(text(formatBytes(hardware.physicalMemoryBytes)).color(WHITE)));
        
        // CPU Load
        sender.sendMessage(text(""));
        sender.sendMessage(text("▸ System Load").color(AQUA));
        sender.sendMessage(text("  CPU Load:   ").color(GRAY).append(text(DF.format(osBean.getSystemLoadAverage())).color(WHITE)));
        sender.sendMessage(text("  Process CPU:").color(GRAY).append(text(getProcessCpuPercent(osBean) + "%").color(WHITE)));

        // Runtime Mode
        sender.sendMessage(text(""));
        sender.sendMessage(text("▸ Runtime Mode").color(AQUA));
        sender.sendMessage(text("  Mode:       ").color(GRAY).append(text(CoreConfig.runtimeMode).color(YELLOW)));
        sender.sendMessage(text("  Safe Mode:  ").color(GRAY).append(text(CoreConfig.isSafeMode() ? "yes" : "no").color(CoreConfig.isSafeMode() ? RED : GREEN)));
        
        // Active Features (from config)
        sender.sendMessage(text(""));
        sender.sendMessage(text("▸ Active Features").color(AQUA));
        listActiveFeatures(sender);

        return true;
    }

    private String getLeviathanVersion() {
        try {
            return dev.vospek.leviathan.version.LeviathanVersionFetcher.getVersion();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getLeafVersion() {
        try {
            return io.papermc.paper.ServerBuildInfo.buildInfo().mcVersion();
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
        sender.sendMessage(text("    Diagnostics:     ").color(GRAY).append(text(CoreConfig.diagnosticsEnabled ? "enabled" : "disabled").color(CoreConfig.diagnosticsEnabled ? GREEN : RED)));
        sender.sendMessage(text("    Observability:   ").color(GRAY).append(text(CoreConfig.observabilityEnabled ? "enabled" : "disabled").color(CoreConfig.observabilityEnabled ? GREEN : RED)));
        sender.sendMessage(text("    Benchmark:       ").color(GRAY).append(text(CoreConfig.benchmarkEnabled ? "enabled" : "disabled").color(CoreConfig.benchmarkEnabled ? GREEN : RED)));
        sender.sendMessage(text("    Experimental:    ").color(GRAY).append(text(CoreConfig.experimentalEnabled ? "enabled" : "disabled").color(CoreConfig.experimentalEnabled ? YELLOW : GRAY)));
        
        sender.sendMessage(text("  Feature Flags:").color(GRAY));
        sender.sendMessage(text("    Linear Storage:  ").color(GRAY).append(text(CoreConfig.featureLinearStorage).color(getFeatureColor(CoreConfig.featureLinearStorage))));
        sender.sendMessage(text("    Zstd Storage:    ").color(GRAY).append(text(CoreConfig.featureZstdStorage).color(getFeatureColor(CoreConfig.featureZstdStorage))));
        sender.sendMessage(text("    DAB:             ").color(GRAY).append(text(CoreConfig.featureDAB).color(getFeatureColor(CoreConfig.featureDAB))));
        sender.sendMessage(text("    Async Chunk:     ").color(GRAY).append(text(CoreConfig.featureAsyncChunk).color(getFeatureColor(CoreConfig.featureAsyncChunk))));
        sender.sendMessage(text("    Region Tick:     ").color(GRAY).append(text(CoreConfig.featureRegionTick).color(getFeatureColor(CoreConfig.featureRegionTick))));
        sender.sendMessage(text("    Plugin Async:    ").color(GRAY).append(text(CoreConfig.featurePluginAsync).color(getFeatureColor(CoreConfig.featurePluginAsync))));
        sender.sendMessage(text("    Hopper Sleep:    ").color(GRAY).append(text(CoreConfig.featureHopperSleep).color(getFeatureColor(CoreConfig.featureHopperSleep))));
        sender.sendMessage(text("    SIMD:            ").color(GRAY).append(text(CoreConfig.featureSIMD).color(getFeatureColor(CoreConfig.featureSIMD))));
        sender.sendMessage(text("    Zstd Network:    ").color(GRAY).append(text(CoreConfig.featureZstdNetwork).color(getFeatureColor(CoreConfig.featureZstdNetwork))));
        sender.sendMessage(text("    Mmap:            ").color(GRAY).append(text(CoreConfig.featureMmap).color(getFeatureColor(CoreConfig.featureMmap))));
        sender.sendMessage(text("    RocksDB:         ").color(GRAY).append(text(CoreConfig.featureRocksDB).color(getFeatureColor(CoreConfig.featureRocksDB))));
    }

    private NamedTextColor getFeatureColor(String flag) {
        return switch (flag) {
            case "enabled" -> GREEN;
            case "experimental" -> YELLOW;
            case "safe" -> AQUA;
            default -> RED; // disabled
        };
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