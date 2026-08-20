package dev.vospek.leviathan.command.subcommands;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

import ca.spottedleaf.common.time.TickData;
import dev.vospek.leviathan.command.LeviathanCommand;
import dev.vospek.leviathan.command.PermissionedLeviathanSubcommand;
import dev.vospek.leviathan.config.modules.async.SparklyPaperParallelWorldTicking;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

/**
 * /leviathan stats 子命令
 * <p>
 * 展示服务器 TPS、CPU、内存、GC、线程与实体/区块等运行状态。
 */
public final class StatsCommand extends PermissionedLeviathanSubcommand {

    /** 子命令字面量 */
    public static final String LITERAL_ARGUMENT = "stats";
    /** 该子命令所需的权限节点 */
    public static final String PERM = LeviathanCommand.BASE_PERM + "." + LITERAL_ARGUMENT;
    private static final DecimalFormat DF = new DecimalFormat("########0.0");

    public StatsCommand() {
        super(PERM, PermissionDefault.TRUE);
    }

    @Override
    public boolean execute(
        final CommandSender sender, final String subCommand, final String[] args
    ) {
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
            .append(text(" ━━━━━━━━━━━━━").color(GOLD));
    }

    private static Component section(String title) {
        return text("▸ ").color(AQUA).append(text(title).color(AQUA));
    }

    private static Component kv(
        String key, Component value, NamedTextColor keyColor, NamedTextColor valueColor
    ) {
        return text(key).color(keyColor).append(value.color(valueColor));
    }

    private void displayTPS(CommandSender sender, MinecraftServer server) {
        sender.sendMessage(section("Tick Performance"));

        // Server-wide TPS (5s, 10s, 60s) - use TickData like MSPTCommand
        double[] tps5s = getTPS(server.tickTimes5s, server);
        double[] tps10s = getTPS(server.tickTimes10s, server);
        double[] tps60s = getTPS(server.tickTimes1m, server);

        sender.sendMessage(tpsLine("TPS (5s):", tps5s));
        sender.sendMessage(tpsLine("TPS (10s):", tps10s));
        sender.sendMessage(tpsLine("TPS (60s):", tps60s));

        // Per-world if parallel ticking enabled
        if (SparklyPaperParallelWorldTicking.enabled) {
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                double[] worldTPS = getTPS(level.tickTimes5s.getTimes());
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
            .append(text(")").color(GRAY));
    }

    private Component worldTpsLine(String worldName, double[] tps) {
        NamedTextColor color = getTPSColor(tps[0]);
        return text("    " + worldName + ": ").color(GRAY)
            .append(text(DF.format(tps[0])).color(color))
            .append(text(" TPS  (").color(GRAY))
            .append(text(DF.format(tps[1]) + " ms").color(color))
            .append(text(")").color(GRAY));
    }

    private double[] getTPS(TickData tickData, MinecraftServer server) {
        TickData.TickReportData reportData = tickData.generateTickReport(
            null, System.nanoTime(), server.tickRateManager().nanosecondsPerTick()
        );
        double avgD = reportData == null ? 0.0
            : reportData.timePerTickData().segmentAll().average() * 1.0E-6D;
        double tps = avgD > 0 ? Math.min(1000.0 / avgD, 20.0) : 20.0;
        return new double[]{tps, avgD};
    }

    private double[] getTPS(long[] times) {
        long min = Long.MAX_VALUE;
        long max = 0L;
        long total = 0L;
        int count = 0;

        for (long value : times) {
            if (value > 0L) {
                count++;
                if (value < min) min = value;
                if (value > max) max = value;
                total += value;
            }
        }

        if (count == 0) {
            return new double[]{20.0, 0.0};
        }

        double avgMs = (total / (double) count) * 1.0E-6D;
        double tps = avgMs > 0 ? Math.min(1000.0 / avgMs, 20.0) : 20.0;
        return new double[]{tps, avgMs};
    }

    private NamedTextColor getTPSColor(double tps) {
        if (tps >= 19.0) return GREEN;
        if (tps >= 17.0) return YELLOW;
        if (tps >= 15.0) return GOLD;
        return RED;
    }

    @SuppressWarnings("deprecation") // getSystemLoadAverage 自 JDK 14 起弃用，只读展示保留兼容
    private void displayCPU(CommandSender sender) {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        sender.sendMessage(section("CPU"));
        sender.sendMessage(
            kv("  System Load: ", text(DF.format(osBean.getSystemLoadAverage())), GRAY, WHITE));
        sender.sendMessage(
            kv("  Process CPU: ", text(getProcessCpuPercent(osBean) + "%"), GRAY, WHITE));
        String available = Runtime.getRuntime().availableProcessors() + " cores";
        sender.sendMessage(kv("  Available:   ", text(available), GRAY, WHITE));
    }

    private double getProcessCpuPercent(OperatingSystemMXBean osBean) {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            return Math.max(0.0, sunBean.getProcessCpuLoad()) * 100.0;
        }
        return 0.0;
    }

    private void displayMemory(CommandSender sender) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        String heapUsedFormatted = formatBytes(heapUsage.getUsed());
        String heapMaxFormatted = formatBytes(heapUsage.getMax());
        String heapSummary = heapUsedFormatted + " / " + heapMaxFormatted
            + " (" + DF.format(heapUsage.getUsed() * 100.0 / heapUsage.getMax()) + "%)";

        sender.sendMessage(section("Memory"));
        sender.sendMessage(kv("  Heap:     ", text(heapSummary), GRAY, WHITE));
        sender.sendMessage(
            kv("  Committed:", text(formatBytes(heapUsage.getCommitted())), GRAY, WHITE));
        sender.sendMessage(
            kv("  Non-Heap: ", text(formatBytes(nonHeapUsage.getUsed())), GRAY, WHITE));
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
                .append(text(DF.format(time) + " ms").color(WHITE)));
        }

        String totalLine = totalCollections + " collections, " + DF.format(totalTime) + " ms";
        sender.sendMessage(kv("  Total: ", text(totalLine), GRAY, WHITE));
    }

    private void displayThreads(CommandSender sender) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        double totalCpuSeconds = getTotalThreadCpuTimeSeconds(threadMXBean);

        sender.sendMessage(section("Threads"));
        String live = String.valueOf(threadMXBean.getThreadCount());
        sender.sendMessage(kv("  Live:     ", text(live), GRAY, WHITE));
        String peak = String.valueOf(threadMXBean.getPeakThreadCount());
        sender.sendMessage(kv("  Peak:     ", text(peak), GRAY, WHITE));
        String daemon = String.valueOf(threadMXBean.getDaemonThreadCount());
        sender.sendMessage(kv("  Daemon:   ", text(daemon), GRAY, WHITE));
        String totalCpu = DF.format(totalCpuSeconds) + " s";
        sender.sendMessage(kv("  Total CPU:", text(totalCpu), GRAY, WHITE));
    }

    private double getTotalThreadCpuTimeSeconds(ThreadMXBean threadMXBean) {
        if (threadMXBean instanceof com.sun.management.ThreadMXBean sunBean) {
            long total = 0;
            long[] times = sunBean.getThreadCpuTime(threadMXBean.getAllThreadIds());
            for (long time : times) {
                if (time > 0) total += time;
            }
            return total / 1_000_000_000.0;
        }
        return 0.0;
    }

    private void displayCounts(CommandSender sender, MinecraftServer server) {
        int players = Bukkit.getOnlinePlayers().size();
        int entities = 0;
        int chunks = 0;

        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            entities += level.getEntityCount();
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
    public List<String> tabComplete(
        final CommandSender sender, final String subCommand, final String[] args
    ) {
        return List.of();
    }
}