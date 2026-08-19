package dev.vospek.leviathan.command.subcommands;

import ca.spottedleaf.common.time.TickData;
import dev.vospek.leviathan.command.LeviathanCommand;
import dev.vospek.leviathan.command.PermissionedLeviathanSubcommand;
import dev.vospek.leviathan.config.modules.async.SparklyPaperParallelWorldTicking;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.List;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class StatsCommand extends PermissionedLeviathanSubcommand {

    public static final String LITERAL_ARGUMENT = "stats";
    public static final String PERM = LeviathanCommand.BASE_PERM + "." + LITERAL_ARGUMENT;
    private static final DecimalFormat DF = new DecimalFormat("########0.0");

    public StatsCommand() {
        super(PERM, PermissionDefault.TRUE);
    }

    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        MinecraftServer server = MinecraftServer.getServer();

        sender.sendMessage(header("Leviathan Server Stats"));
        sender.sendMessage(empty());

        // TPS / MSPT
        displayTPS(sender, server);
        sender.sendMessage(empty());

        // CPU
        displayCPU(sender);
        sender.sendMessage(empty());

        // Memory
        displayMemory(sender);
        sender.sendMessage(empty());

        // GC
        displayGC(sender);
        sender.sendMessage(empty());

        // Threads
        displayThreads(sender);
        sender.sendMessage(empty());

        // Players, Entities, Chunks
        displayCounts(sender, server);

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

    private void displayTPS(CommandSender sender, MinecraftServer server) {
        sender.sendMessage(section("Tick Performance"));

        // Server-wide TPS (5s, 10s, 60s) - use TickData like MSPTCommand
        double[] tps5s = getTPS(server.tickTimes5s);
        double[] tps10s = getTPS(server.tickTimes10s);
        double[] tps60s = getTPS(server.tickTimes1m);

        sender.sendMessage(tpsLine("TPS (5s):", tps5s));
        sender.sendMessage(tpsLine("TPS (10s):", tps10s));
        sender.sendMessage(tpsLine("TPS (60s):", tps60s));

        // Per-world if parallel ticking enabled
        if (SparklyPaperParallelWorldTicking.enabled) {
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                double[] worldTPS = getTPS(level.tickTimes5s);
                sender.sendMessage(worldTpsLine(level.getWorld().getName(), worldTPS));
            }
        }
    }

    private Component tpsLine(String label, double[] tps) {
        NamedTextColor color = getTPSColor(tps[0]);
        return text("  " + label + " ").color(GRAY)
            .append(text(DF.format(tps[0])).color(color))
            .append(text("  (").color(GRAY))
            .append(text(DF.format(tps[1]) + " ms").color(color))
            .append(text(")").color(GRAY))
            .build();
    }

    private Component worldTpsLine(String worldName, double[] tps) {
        NamedTextColor color = getTPSColor(tps[0]);
        return text("    " + worldName + ": ").color(GRAY)
            .append(text(DF.format(tps[0])).color(color))
            .append(text(" TPS  (").color(GRAY))
            .append(text(DF.format(tps[1]) + " ms").color(color))
            .append(text(")").color(GRAY))
            .build();
    }

    private double[] getTPS(TickData tickData) {
        TickData.TickReportData reportData = tickData.generateTickReport(null, System.nanoTime(), MinecraftServer.getServer().tickRateManager().nanosecondsPerTick());
        double avgD = reportData == null ? 0.0 : reportData.timePerTickData().segmentAll().average() * 1.0E-6D;
        double minD = reportData == null ? 0.0 : reportData.timePerTickData().segmentAll().least() * 1.0E-6D;
        double maxD = reportData == null ? 0.0 : reportData.timePerTickData().segmentAll().greatest() * 1.0E-6D;
        double tps = avgD > 0 ? Math.min(1000.0 / avgD, 20.0) : 20.0;
        return new double[]{tps, avgD};
    }

    private NamedTextColor getTPSColor(double tps) {
        if (tps >= 19.0) return GREEN;
        if (tps >= 17.0) return YELLOW;
        if (tps >= 15.0) return GOLD;
        return RED;
    }

    private void displayCPU(CommandSender sender) {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        sender.sendMessage(section("CPU"));
        sender.sendMessage(kv("  System Load: ", text(DF.format(osBean.getSystemLoadAverage())), GRAY, WHITE));
        sender.sendMessage(kv("  Process CPU: ", text(getProcessCpuPercent(osBean) + "%"), GRAY, WHITE));
        sender.sendMessage(kv("  Available:   ", text(Runtime.getRuntime().availableProcessors() + " cores"), GRAY, WHITE));
    }

    private double getProcessCpuPercent(OperatingSystemMXBean osBean) {
        try {
            Double load = (Double) osBean.getClass().getMethod("getProcessCpuLoad").invoke(osBean);
            return load != null ? load * 100.0 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void displayMemory(CommandSender sender) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        sender.sendMessage(section("Memory"));
        sender.sendMessage(kv("  Heap:     ", text(formatBytes(heapUsage.getUsed()) + " / " + formatBytes(heapUsage.getMax()) + " (" + DF.format(heapUsage.getUsed() * 100.0 / heapUsage.getMax()) + "%)"), GRAY, WHITE));
        sender.sendMessage(kv("  Committed:", text(formatBytes(heapUsage.getCommitted())), GRAY, WHITE));
        sender.sendMessage(kv("  Non-Heap: ", text(formatBytes(nonHeapUsage.getUsed())), GRAY, WHITE));
    }

    private void displayGC(CommandSender sender) {
        sender.sendMessage(section("Garbage Collection"));
        long totalCollections = 0;
        long totalTime = 0;

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();
            totalCollections += count;
            totalTime += time;
            sender.sendMessage(text("  " + gcBean.getName() + ": ").color(GRAY)
                .append(text(count + " collections").color(WHITE))
                .append(text(", ").color(GRAY))
                .append(text(DF.format(time) + " ms").color(WHITE))
                .build());
        }

        sender.sendMessage(kv("  Total: ", text(totalCollections + " collections, " + DF.format(totalTime) + " ms"), GRAY, WHITE));
    }

    private void displayThreads(CommandSender sender) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        sender.sendMessage(section("Threads"));
        sender.sendMessage(kv("  Live:     ", text(String.valueOf(threadMXBean.getThreadCount())), GRAY, WHITE));
        sender.sendMessage(kv("  Peak:     ", text(String.valueOf(threadMXBean.getPeakThreadCount())), GRAY, WHITE));
        sender.sendMessage(kv("  Daemon:   ", text(String.valueOf(threadMXBean.getDaemonThreadCount())), GRAY, WHITE));
        sender.sendMessage(kv("  Total CPU:", text(DF.format(getTotalThreadCpuTimeSeconds(threadMXBean)) + " s"), GRAY, WHITE));
    }

    private double getTotalThreadCpuTimeSeconds(ThreadMXBean threadMXBean) {
        try {
            Long time = (Long) threadMXBean.getClass().getMethod("getTotalThreadCpuTime").invoke(threadMXBean);
            return time != null ? time / 1_000_000_000.0 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void displayCounts(CommandSender sender, MinecraftServer server) {
        int players = Bukkit.getOnlinePlayers().size();
        int entities = 0;
        int chunks = 0;

        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            entities += level.getEntities().size();
            chunks += level.getChunkSource().getLoadedChunksCount();
        }

        sender.sendMessage(section("Counts"));
        sender.sendMessage(kv("  Players: ", text(String.valueOf(players)), GRAY, WHITE));
        sender.sendMessage(kv("  Entities:", text(String.valueOf(entities)), GRAY, WHITE));
        sender.sendMessage(kv("  Chunks:  ", text(String.valueOf(chunks)), GRAY, WHITE));
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