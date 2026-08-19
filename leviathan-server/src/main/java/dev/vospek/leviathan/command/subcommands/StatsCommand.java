package dev.vospek.leviathan.command.subcommands;

import ca.spottedleaf.common.time.TickData;
import dev.vospek.leviathan.command.LeviathanCommand;
import dev.vospek.leviathan.command.PermissionedLeviathanSubcommand;
import dev.vospek.leviathan.config.modules.async.SparklyPaperParallelWorldTicking;
import net.kyori.adventure.text.Component;
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

        sender.sendMessage(text("━━━━━━━━━━━━━ ").color(GOLD)
            .append(text("Leviathan Server Stats").color(YELLOW))
            .append(text(" ━━━━━━━━━━━━━").color(GOLD))
            .build());

        sender.sendMessage(text(""));

        // TPS / MSPT
        displayTPS(sender, server);

        sender.sendMessage(text(""));

        // CPU
        displayCPU(sender);

        sender.sendMessage(text(""));

        // Memory
        displayMemory(sender);

        sender.sendMessage(text(""));

        // GC
        displayGC(sender);

        sender.sendMessage(text(""));

        // Threads
        displayThreads(sender);

        sender.sendMessage(text(""));

        // Players, Entities, Chunks
        displayCounts(sender, server);

        return true;
    }

    private void displayTPS(CommandSender sender, MinecraftServer server) {
        sender.sendMessage(text("▸ Tick Performance").color(AQUA));

        // Server-wide TPS (5s, 10s, 60s) - use TickData like MSPTCommand
        double[] tps5s = getTPS(server.tickTimes5s);
        double[] tps10s = getTPS(server.tickTimes10s);
        double[] tps60s = getTPS(server.tickTimes1m);

        sender.sendMessage(text("  TPS (5s):  ").color(GRAY)
            .append(text(DF.format(tps5s[0])).color(getTPSColor(tps5s[0])))
            .append(text("  (").color(GRAY))
            .append(text(DF.format(tps5s[1]) + " ms").color(getTPSColor(tps5s[0])))
            .append(text(")").color(GRAY))
            .build());

        sender.sendMessage(text("  TPS (10s): ").color(GRAY)
            .append(text(DF.format(tps10s[0])).color(getTPSColor(tps10s[0])))
            .append(text("  (").color(GRAY))
            .append(text(DF.format(tps10s[1]) + " ms").color(getTPSColor(tps10s[0])))
            .append(text(")").color(GRAY))
            .build());

        sender.sendMessage(text("  TPS (60s): ").color(GRAY)
            .append(text(DF.format(tps60s[0])).color(getTPSColor(tps60s[0])))
            .append(text("  (").color(GRAY))
            .append(text(DF.format(tps60s[1]) + " ms").color(getTPSColor(tps60s[0])))
            .append(text(")").color(GRAY))
            .build());

        // Per-world if parallel ticking enabled
        if (SparklyPaperParallelWorldTicking.enabled) {
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                double[] worldTPS = getTPS(level.tickTimes5s);
                sender.sendMessage(text("    " + level.getWorld().getName() + ": ").color(GRAY)
                    .append(text(DF.format(worldTPS[0])).color(getTPSColor(worldTPS[0])))
                    .append(text(" TPS  (").color(GRAY))
                    .append(text(DF.format(worldTPS[1]) + " ms").color(getTPSColor(worldTPS[0])))
                    .append(text(")").color(GRAY))
                    .build());
            }
        }
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
        sender.sendMessage(text("▸ CPU").color(AQUA));
        sender.sendMessage(text("  System Load: ").color(GRAY)
            .append(text(DF.format(osBean.getSystemLoadAverage())).color(WHITE))
            .build());
        sender.sendMessage(text("  Process CPU: ").color(GRAY)
            .append(text(DF.format(osBean.getProcessCpuLoad() * 100) + "%").color(WHITE))
            .build());
        sender.sendMessage(text("  Available:   ").color(GRAY)
            .append(text(String.valueOf(Runtime.getRuntime().availableProcessors()) + " cores").color(WHITE))
            .build());
    }

    private void displayMemory(CommandSender sender) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        sender.sendMessage(text("▸ Memory").color(AQUA));
        sender.sendMessage(text("  Heap:     ").color(GRAY)
            .append(text(formatBytes(heapUsage.getUsed()) + " / " + formatBytes(heapUsage.getMax())).color(WHITE))
            .append(text(" (" + DF.format(heapUsage.getUsed() * 100.0 / heapUsage.getMax()) + "%)").color(GRAY))
            .build());
        sender.sendMessage(text("  Committed:").color(GRAY)
            .append(text(formatBytes(heapUsage.getCommitted())).color(WHITE))
            .build());
        sender.sendMessage(text("  Non-Heap: ").color(GRAY)
            .append(text(formatBytes(nonHeapUsage.getUsed())).color(WHITE))
            .build());
    }

    private void displayGC(CommandSender sender) {
        sender.sendMessage(text("▸ Garbage Collection").color(AQUA));
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

        sender.sendMessage(text("  Total: ").color(GRAY)
            .append(text(totalCollections + " collections").color(WHITE))
            .append(text(", ").color(GRAY))
            .append(text(DF.format(totalTime) + " ms").color(WHITE))
            .build());
    }

    private void displayThreads(CommandSender sender) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        sender.sendMessage(text("▸ Threads").color(AQUA));
        sender.sendMessage(text("  Live:     ").color(GRAY)
            .append(text(String.valueOf(threadMXBean.getThreadCount())).color(WHITE))
            .build());
        sender.sendMessage(text("  Peak:     ").color(GRAY)
            .append(text(String.valueOf(threadMXBean.getPeakThreadCount())).color(WHITE))
            .build());
        sender.sendMessage(text("  Daemon:   ").color(GRAY)
            .append(text(String.valueOf(threadMXBean.getDaemonThreadCount())).color(WHITE))
            .build());
        sender.sendMessage(text("  Total CPU:").color(GRAY)
            .append(text(DF.format(threadMXBean.getTotalThreadCpuTime() / 1_000_000_000.0) + " s").color(WHITE))
            .build());
    }

    private void displayCounts(CommandSender sender, MinecraftServer server) {
        int players = Bukkit.getOnlinePlayers().size();
        int entities = 0;
        int chunks = 0;

        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            entities += level.getEntities().size();
            chunks += level.getChunkSource().getLoadedChunksCount();
        }

        sender.sendMessage(text("▸ Counts").color(AQUA));
        sender.sendMessage(text("  Players: ").color(GRAY)
            .append(text(String.valueOf(players)).color(WHITE))
            .build());
        sender.sendMessage(text("  Entities:").color(GRAY)
            .append(text(String.valueOf(entities)).color(WHITE))
            .build());
        sender.sendMessage(text("  Chunks:  ").color(GRAY)
            .append(text(String.valueOf(chunks)).color(WHITE))
            .build());
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