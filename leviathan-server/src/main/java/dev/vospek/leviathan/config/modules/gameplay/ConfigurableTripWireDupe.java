package dev.vospek.leviathan.config.modules.gameplay;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class ConfigurableTripWireDupe extends ConfigModule {

    public String basePath() {
        return ConfigCategory.GAMEPLAY.basePath();
    }

    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath() + ".allow-tripwire-dupe", enabled);
    }
}
