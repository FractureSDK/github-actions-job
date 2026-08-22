package dev.vospek.leviathan.command;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

public interface LeviathanSubcommand {

    boolean execute(CommandSender sender, String subCommand, String[] args);

    default List<String> tabComplete(
        final CommandSender sender,
        final String subCommand,
        final String[] args
    ) {
        return Collections.emptyList();
    }

    boolean testPermission(CommandSender sender);

    Permission getPermission();
}
