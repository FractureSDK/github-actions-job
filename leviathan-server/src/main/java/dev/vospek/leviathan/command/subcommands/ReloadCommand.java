package dev.vospek.leviathan.command.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.MinecraftServer;
import dev.vospek.leviathan.command.LeviathanCommand;
import dev.vospek.leviathan.command.PermissionedLeviathanSubcommand;
import dev.vospek.leviathan.config.LeviathanConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.permissions.PermissionDefault;

public final class ReloadCommand extends PermissionedLeviathanSubcommand {

    public final static String LITERAL_ARGUMENT = "reload";
    public static final String PERM = LeviathanCommand.BASE_PERM + "." + LITERAL_ARGUMENT;

    public ReloadCommand() {
        super(PERM, PermissionDefault.OP);
    }

    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        this.doGaleReload(sender);
        this.doLeviathanReload(sender);
        return true;
    }

    // Gale start - Gale commands - /gale reload command
    private void doGaleReload(final CommandSender sender) {
        Command.broadcastCommandMessage(sender, Component.text("Reloading Gale config...", NamedTextColor.GREEN));

        MinecraftServer server = ((CraftServer) sender.getServer()).getServer();
        server.galeConfigurations.reloadConfigs(server);
        server.server.reloadCount++;

        Command.broadcastCommandMessage(sender, Component.text("Gale config reload complete.", NamedTextColor.GREEN));
    }
    // Gale end - Gale commands - /gale reload command

    private void doLeviathanReload(final CommandSender sender) {
        Command.broadcastCommandMessage(sender, Component.text("Reloading Leviathan config...", NamedTextColor.GREEN));

        LeviathanConfig.reloadAsync(sender);
    }
}
