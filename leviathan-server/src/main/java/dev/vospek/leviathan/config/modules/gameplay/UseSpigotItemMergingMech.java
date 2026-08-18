package dev.vospek.leviathan.config.modules.gameplay;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class UseSpigotItemMergingMech extends ConfigModule {

    public String basePath() {
        return ConfigCategory.GAMEPLAY.basePath() + ".use-spigot-item-merging-mechanism";
    }

    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath(), enabled);
    }
}
