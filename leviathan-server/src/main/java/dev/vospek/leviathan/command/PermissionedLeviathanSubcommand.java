package dev.vospek.leviathan.command;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public abstract class PermissionedLeviathanSubcommand implements LeviathanSubcommand {

    private final Permission permission;

    protected PermissionedLeviathanSubcommand(Permission permission) {
        this.permission = permission;
    }

    protected PermissionedLeviathanSubcommand(String permission, PermissionDefault permissionDefault) {
        this(new Permission(permission, permissionDefault));
    }

    @Override
    public boolean testPermission(CommandSender sender) {
        return sender.hasPermission(this.permission);
    }

    @Override
    public Permission getPermission() {
        return this.permission;
    }
}
