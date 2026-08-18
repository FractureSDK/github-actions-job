package dev.vospek.leviathan.config.modules.misc;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class Including5sIngetTPS extends ConfigModule {

    public String basePath() {
        return ConfigCategory.MISC.basePath();
    }

    public static boolean enabled = true;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath() + ".including-5s-in-get-tps", enabled);
    }
}
