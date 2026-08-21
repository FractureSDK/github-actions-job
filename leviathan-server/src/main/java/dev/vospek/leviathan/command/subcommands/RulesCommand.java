package dev.vospek.leviathan.command.subcommands;

import dev.vospek.leviathan.bootstrap.LeviathanBootstrap;
import dev.vospek.leviathan.command.LeviathanCommand;
import dev.vospek.leviathan.command.PermissionedLeviathanSubcommand;
import dev.vospek.leviathan.observability.StartupReporter;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.Collections;
import java.util.List;

/**
 * {@code /leviathan rules} 子命令
 * <p>
 * 展示按域分组的 Feature Flag 有效状态（safe 模式将 experimental 降级为 disabled），
 * 以及核心开关与运行时模式。对应 Phase 1-G (W7-01/W7-02/W7-03)：
 * 控制类功能的统一入口与最终规则展示。
 */
public final class RulesCommand extends PermissionedLeviathanSubcommand {

    public static final String LITERAL_ARGUMENT = "rules";
    public static final String PERM = LeviathanCommand.BASE_PERM + "." + LITERAL_ARGUMENT;

    public RulesCommand() {
        super(PERM, PermissionDefault.OP);
    }

    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        LeviathanBootstrap.initialize();
        sender.sendMessage(StartupReporter.buildEffectiveFeatures());
        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String subCommand, final String[] args) {
        return Collections.emptyList();
    }
}