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

import static net.kyori.adventure.text.Component.empty;
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

        sender.sendMessage(header("Leviathan Runtime & Hardware"));

        // Java Runtime
        sender.sendMessage(section("Java Runtime"));
        sender.sendMessage(kv("  Version: ", text(runtime.javaVersion), GRAY, WHITE));
        sender.sendMessage(kv("  Vendor:  ", text(runtime.javaVendor), GRAY, WHITE));
        sender.sendMessage(kv("  VM:      ", text(runtime.vmName + " " + runtime.vmVersion), GRAY, WHITE));
        sender.sendMessage(kv("  Home:    ", text(runtime.javaHome), GRAY, WHITE));

        // OS & Architecture
        sender.sendMessage(empty());
        sender.sendMessage(section("Operating System"));
        sender.sendMessage(kv("  OS:      ", text(runtime.osName + " " + runtime.osVersion), GRAY, WHITE));
        sender.sendMessage(kv("  Arch:    ", text(runtime.architecture), GRAY, WHITE));
        sender.sendMessage(kv("  Kernel:  ", text(runtime.kernelVersion), GRAY, WHITE));

        // CPU
        sender.sendMessage(empty());
        sender.sendMessage(section("CPU"));
        sender.sendMessage(kv("  Logical:     ", text(String.valueOf(hardware.logicalProcessors)), GRAY, WHITE));
        sender.sendMessage(kv("  Physical:    ", text(String.valueOf(hardware.physicalProcessors)), GRAY, WHITE));
        sender.sendMessage(kv("  Vendor:      ", text(hardware.cpuVendor), GRAY, WHITE));
        sender.sendMessage(kv("  Model:       ", text(hardware.cpuModel), GRAY, WHITE));
        sender.sendMessage(kv("  SIMD:        ", text(hardware.hasSIMD ? "yes" : "no"), GRAY, hardware.hasSIMD ? GREEN : RED));
        sender.sendMessage(kv("  AVX2:        ", text(hardware.hasAVX2 ? "yes" : "no"), GRAY, hardware.hasAVX2 ? GREEN : RED));
        sender.sendMessage(kv("  AVX-512:     ", text(hardware.hasAVX512 ? "yes" : "no"), GRAY, hardware.hasAVX512 ? GREEN : RED));

        // Memory
        sender.sendMessage(empty());
        sender.sendMessage(section("Memory"));
        sender.sendMessage(kv("  Physical:    ", text(formatBytes(hardware.physicalMemoryBytes)), GRAY, WHITE));
        sender.sendMessage(kv("  Max Heap:    ", text(formatBytes(hardware.maxHeapBytes)), GRAY, WHITE));
        sender.sendMessage(kv("  Direct Mem:  ", text(formatBytes(hardware.directMemoryBytes)), GRAY, WHITE));

        // Current Memory Usage
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        sender.sendMessage(kv("  Heap Used:   ", text(formatBytes(heapUsage.getUsed())), GRAY, WHITE));
        sender.sendMessage(kv("  Heap Committed:", text(formatBytes(heapUsage.getCommitted())), GRAY, WHITE));
        sender.sendMessage(kv("  Non-Heap:    ", text(formatBytes(nonHeapUsage.getUsed())), GRAY, WHITE));

        // JVM Arguments
        sender.sendMessage(empty());
        sender.sendMessage(section("JVM Arguments"));
        for (String arg : runtime.inputArguments) {
            sender.sendMessage(text("  ").color(GRAY).append(text(arg).color(GRAY)).build());
        }

        return true;
    }

    private static Component header(String title) {
        return text("━━━━━━━━━━━━━ ").color(GOLD)
            .append(text(title).color(YELLOW))
            .append(text(" ━━━━━━━━━━━━━").color(GOLD))
            .build();
    }

    private static Component section(String title) {
        return text("▸ ").color(AQUA).append(text(title).color(AQUA)).build();
    }

    private static Component kv(String key, Component value, NamedTextColor keyColor, NamedTextColor valueColor) {
        return text(key).color(keyColor).append(value.color(valueColor)).build();
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