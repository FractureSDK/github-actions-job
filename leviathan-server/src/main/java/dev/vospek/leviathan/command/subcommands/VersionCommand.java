package dev.vospek.leviathan.command.subcommands;

import dev.vospek.leviathan.command.LeviathanCommand;
import dev.vospek.leviathan.command.PermissionedLeviathanSubcommand;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

public final class VersionCommand extends PermissionedLeviathanSubcommand {

    public static final String LITERAL_ARGUMENT = "version";
    public static final String PERM = LeviathanCommand.BASE_PERM + "." + LITERAL_ARGUMENT;

    public VersionCommand() {
        super(PERM, PermissionDefault.TRUE);
    }

    @Override
    public boolean execute(
        final CommandSender sender,
        final String subCommand,
        final String[] args
    ) {
        final Command ver = MinecraftServer.getServer()
            .server.getCommandMap().getCommand("version");

        if (ver != null) {
            // Gale - JettPack - reduce array allocations
            ver.execute(
                sender, LeviathanCommand.COMMAND_LABEL,
                me.titaniumtown.ArrayConstants.emptyStringArray);
        }

        return true;
    }

    @Override
    public boolean testPermission(CommandSender sender) {
        return super.testPermission(sender) && sender.hasPermission("bukkit.command.version");
    }
}