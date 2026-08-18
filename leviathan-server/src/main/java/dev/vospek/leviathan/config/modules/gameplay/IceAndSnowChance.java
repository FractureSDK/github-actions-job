package dev.vospek.leviathan.config.modules.gameplay;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class IceAndSnowChance extends ConfigModule {

    public String basePath() {
        return ConfigCategory.GAMEPLAY.basePath() + ".ice-and-snow-chance";
    }

    public static int iceAndSnowChance = 48;

    @Override
    public void onLoaded() {
        iceAndSnowChance = globalConfig.getInt(basePath(), iceAndSnowChance);
        if (iceAndSnowChance <= 0) {
            iceAndSnowChance = 48;
        }
    }
}
