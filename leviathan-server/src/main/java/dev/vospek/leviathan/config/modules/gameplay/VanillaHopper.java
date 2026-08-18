package dev.vospek.leviathan.config.modules.gameplay;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class VanillaHopper extends ConfigModule {

    public String basePath() {
        return ConfigCategory.GAMEPLAY.basePath() + ".use-vanilla-hopper";
    }

    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath(), enabled);
    }
}
