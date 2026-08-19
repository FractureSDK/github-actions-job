package dev.vospek.leviathan.command.subcommands;

import dev.vospek.leviathan.bootstrap.HardwareCapabilities;
import dev.vospek.leviathan.bootstrap.LeviathanBootstrap;
import dev.vospek.leviathan.bootstrap.RuntimeDetector;
import dev.vospek.leviathan.command.LeviathanCommand;
import dev.vospek.leviathan.command.PermissionedLeviathanSubcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.List;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class RuntimeCommand extends PermissionedLeviathanSubcommand {

    public static final String LITERAL_ARGUMENT = "runtime";
    public static final String PERM = LeviathanCommand.BASE_PERM + "." + LITERAL_ARGUMENT;
    private static final DecimalFormat DF = new DecimalFormat("########0.00");

    public RuntimeCommand() {
        super(PERM, PermissionDefault.OP);
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

        sender.sendMessage(text("━━━━━━━━━━━━━ ").color(GOLD)
            .append(text("Leviathan Runtime & Hardware").color(YELLOW))
            .append(text(" ━━━━━━━━━━━━━").color(GOLD))
            .build());

        // Java Runtime
        sender.sendMessage(text("").build());
        sender.sendMessage(text("▸ Java Runtime").color(AQUA));
        sender.sendMessage(text("  Version: ").color(GRAY).append(text(runtime.javaVersion).color(WHITE)));
        sender.sendMessage(text("  Vendor:  ").color(GRAY).append(text(runtime.javaVendor).color(WHITE)));
        sender.sendMessage(text("  VM:      ").color(GRAY).append(text(runtime.vmName + " " + runtime.vmVersion).color(WHITE)));
        sender.sendMessage(text("  Home:    ").color(GRAY).append(text(runtime.javaHome).color(WHITE)));

        // OS & Architecture
        sender.sendMessage(text("").build());
        sender.sendMessage(text("▸ Operating System").color(AQUA));
        sender.sendMessage(text("  OS:      ").color(GRAY).append(text(runtime.osName + " " + runtime.osVersion).color(WHITE)));
        sender.sendMessage(text("  Arch:    ").color(GRAY).append(text(runtime.architecture).color(WHITE)));
        sender.sendMessage(text("  Kernel:  ").color(GRAY).append(text(runtime.kernelVersion).color(WHITE)));

        // CPU
        sender.sendMessage(text("").build());
        sender.sendMessage(text("▸ CPU").color(AQUA));
        sender.sendMessage(text("  Logical:     ").color(GRAY).append(text(String.valueOf(hardware.logicalProcessors)).color(WHITE)));
        sender.sendMessage(text("  Physical:    ").color(GRAY).append(text(String.valueOf(hardware.physicalProcessors)).color(WHITE)));
        sender.sendMessage(text("  Vendor:      ").color(GRAY).append(text(hardware.cpuVendor).color(WHITE)));
        sender.sendMessage(text("  Model:       ").color(GRAY).append(text(hardware.cpuModel).color(WHITE)));
        sender.sendMessage(text("  SIMD:        ").color(GRAY).append(text(hardware.hasSIMD ? "yes" : "no").color(hardware.hasSIMD ? GREEN : RED)));
        sender.sendMessage(text("  AVX2:        ").color(GRAY).append(text(hardware.hasAVX2 ? "yes" : "no").color(hardware.hasAVX2 ? GREEN : RED)));
        sender.sendMessage(text("  AVX-512:     ").color(GRAY).append(text(hardware.hasAVX512 ? "yes" : "no").color(hardware.hasAVX512 ? GREEN : RED)));

        // Memory
        sender.sendMessage(text("").build());
        sender.sendMessage(text("▸ Memory").color(AQUA));
        sender.sendMessage(text("  Physical:    ").color(GRAY).append(text(formatBytes(hardware.physicalMemoryBytes)).color(WHITE)));
        sender.sendMessage(text("  Max Heap:    ").color(GRAY).append(text(formatBytes(hardware.maxHeapBytes)).color(WHITE)));
        sender.sendMessage(text("  Direct Mem:  ").color(GRAY).append(text(formatBytes(hardware.directMemoryBytes)).color(WHITE)));

        // Current Memory Usage
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        sender.sendMessage(text("  Heap Used:   ").color(GRAY).append(text(formatBytes(heapUsage.getUsed())).color(WHITE)));
        sender.sendMessage(text("  Heap Committed:").color(GRAY).append(text(formatBytes(heapUsage.getCommitted())).color(WHITE)));
        sender.sendMessage(text("  Non-Heap:    ").color(GRAY).append(text(formatBytes(nonHeapUsage.getUsed())).color(WHITE)));

        // JVM Arguments
        sender.sendMessage(text("").build());
        sender.sendMessage(text("▸ JVM Arguments").color(AQUA));
        for (String arg : runtime.inputArguments) {
            sender.sendMessage(text("  ").color(GRAY).append(text(arg).color(GRAY)));
        }

        return true;
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