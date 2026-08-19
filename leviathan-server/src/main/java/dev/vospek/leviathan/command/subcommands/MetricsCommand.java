package dev.vospek.leviathan.command.subcommands;

import dev.vospek.leviathan.command.LeviathanCommand;
import dev.vospek.leviathan.command.PermissionedLeviathanSubcommand;
import dev.vospek.leviathan.observability.MetricRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MetricsCommand extends PermissionedLeviathanSubcommand {

    public static final String LITERAL_ARGUMENT = "metrics";
    public static final String PERM = LeviathanCommand.BASE_PERM + "." + LITERAL_ARGUMENT;

    public MetricsCommand() {
        super(PERM, PermissionDefault.OP);
    }

    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        if (args.length == 0) {
            showOverview(sender);
            return true;
        }

        String subCmd = args[0].toLowerCase();
        return switch (subCmd) {
            case "counters" -> showCounters(sender);
            case "gauges" -> showGauges(sender);
            case "histograms" -> showHistograms(sender);
            case "timers" -> showTimers(sender);
            case "rates" -> showRates(sender);
            case "snapshot" -> showSnapshot(sender);
            default -> {
                sender.sendMessage(text("Unknown subcommand: " + subCmd).color(RED));
                sender.sendMessage(text("Usage: /leviathan metrics [counters|gauges|histograms|timers|rates|snapshot]").color(GRAY));
                yield false;
            }
        };
    }

    private void showOverview(CommandSender sender) {
        MetricRegistry registry = MetricRegistry.get();
        MetricRegistry.MetricSnapshot snapshot = registry.snapshot();

        sender.sendMessage(text("━━━━━━━━━━━━━ ").color(GOLD)
            .append(text("Leviathan Metrics Registry").color(YELLOW))
            .append(text(" ━━━━━━━━━━━━━").color(GOLD))
            .build());

        sender.sendMessage(text(""));
        sender.sendMessage(text("Registered Metrics:").color(AQUA));
        sender.sendMessage(text("  Counters:    ").color(GRAY).append(text(String.valueOf(snapshot.counters().size())).color(WHITE)));
        sender.sendMessage(text("  Gauges:      ").color(GRAY).append(text(String.valueOf(snapshot.gauges().size())).color(WHITE)));
        sender.sendMessage(text("  Histograms:  ").color(GRAY).append(text(String.valueOf(snapshot.histograms().size())).color(WHITE)));
        sender.sendMessage(text("  Timers:      ").color(GRAY).append(text(String.valueOf(snapshot.timers().size())).color(WHITE)));
        sender.sendMessage(text("  Rates:       ").color(GRAY).append(text(String.valueOf(snapshot.rates().size())).color(WHITE)));

        sender.sendMessage(text(""));
        sender.sendMessage(text("Usage:").color(AQUA));
        sender.sendMessage(text("  /leviathan metrics counters    ").color(GRAY).append(text("- List all counters").color(GRAY)));
        sender.sendMessage(text("  /leviathan metrics gauges      ").color(GRAY).append(text("- List all gauges").color(GRAY)));
        sender.sendMessage(text("  /leviathan metrics histograms  ").color(GRAY).append(text("- List all histograms").color(GRAY)));
        sender.sendMessage(text("  /leviathan metrics timers      ").color(GRAY).append(text("- List all timers").color(GRAY)));
        sender.sendMessage(text("  /leviathan metrics rates       ").color(GRAY).append(text("- List all rates").color(GRAY)));
        sender.sendMessage(text("  /leviathan metrics snapshot    ").color(GRAY).append(text("- Full snapshot").color(GRAY)));
    }

    private void showCounters(CommandSender sender) {
        MetricRegistry.MetricSnapshot snapshot = MetricRegistry.get().snapshot();

        sender.sendMessage(text("━━ Counters ━━").color(GOLD));
        if (snapshot.counters().isEmpty()) {
            sender.sendMessage(text("  (none)").color(GRAY));
            return;
        }

        for (MetricRegistry.Counter counter : snapshot.counters().values()) {
            sender.sendMessage(text("  " + counter.getName() + " = " + counter.get()).color(WHITE));
        }
    }

    private void showGauges(CommandSender sender) {
        MetricRegistry.MetricSnapshot snapshot = MetricRegistry.get().snapshot();

        sender.sendMessage(text("━━ Gauges ━━").color(GOLD));
        if (snapshot.gauges().isEmpty()) {
            sender.sendMessage(text("  (none)").color(GRAY));
            return;
        }

        for (MetricRegistry.Gauge<?> gauge : snapshot.gauges().values()) {
            sender.sendMessage(text("  " + gauge.toString()).color(WHITE));
        }
    }

    private void showHistograms(CommandSender sender) {
        MetricRegistry.MetricSnapshot snapshot = MetricRegistry.get().snapshot();

        sender.sendMessage(text("━━ Histograms ━━").color(GOLD));
        if (snapshot.histograms().isEmpty()) {
            sender.sendMessage(text("  (none)").color(GRAY));
            return;
        }

        for (MetricRegistry.Histogram histogram : snapshot.histograms().values()) {
            sender.sendMessage(text("  " + histogram.toString()).color(WHITE));
        }
    }

    private void showTimers(CommandSender sender) {
        MetricRegistry.MetricSnapshot snapshot = MetricRegistry.get().snapshot();

        sender.sendMessage(text("━━ Timers ━━").color(GOLD));
        if (snapshot.timers().isEmpty()) {
            sender.sendMessage(text("  (none)").color(GRAY));
            return;
        }

        for (MetricRegistry.Timer timer : snapshot.timers().values()) {
            sender.sendMessage(text("  " + timer.toString()).color(WHITE));
        }
    }

    private void showRates(CommandSender sender) {
        MetricRegistry.MetricSnapshot snapshot = MetricRegistry.get().snapshot();

        sender.sendMessage(text("━━ Rates ━━").color(GOLD));
        if (snapshot.rates().isEmpty()) {
            sender.sendMessage(text("  (none)").color(GRAY));
            return;
        }

        for (MetricRegistry.Rate rate : snapshot.rates().values()) {
            sender.sendMessage(text("  " + rate.toString()).color(WHITE));
        }
    }

    private void showSnapshot(CommandSender sender) {
        MetricRegistry.MetricSnapshot snapshot = MetricRegistry.get().snapshot();

        sender.sendMessage(text("━━ Full Snapshot ━━").color(GOLD));
        sender.sendMessage(text(snapshot.toString()).color(GRAY));
        sender.sendMessage(text(""));

        showCounters(sender);
        sender.sendMessage(text(""));
        showGauges(sender);
        sender.sendMessage(text(""));
        showHistograms(sender);
        sender.sendMessage(text(""));
        showTimers(sender);
        sender.sendMessage(text(""));
        showRates(sender);
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String subCommand, final String[] args) {
        if (args.length == 1) {
            return List.of("counters", "gauges", "histograms", "timers", "rates", "snapshot");
        }
        return List.of();
    }
}