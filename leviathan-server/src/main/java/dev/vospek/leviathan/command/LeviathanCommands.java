package dev.vospek.leviathan.command;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.util.permissions.CraftDefaultPermissions;

public final class LeviathanCommands {

    public static final String COMMAND_BASE_PERM =
        CraftDefaultPermissions.LEVIATHAN_ROOT + ".command";

    private static final Map<String, Command> COMMANDS = new HashMap<>();

    static {
        COMMANDS.put(LeviathanCommand.COMMAND_LABEL, new LeviathanCommand());
    }

    private LeviathanCommands() {
    }

    public static void registerCommands(final MinecraftServer server) {
        COMMANDS.forEach(
            (s, command) -> server.server.getCommandMap().register(s, "Leviathan", command));
    }
}