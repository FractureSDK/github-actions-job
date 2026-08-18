package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class SleepingBlockEntity extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath();
    }

    public static boolean enabled = false;
    private static boolean sleepingBlockEntityInitialized;

    @Override
    public void onLoaded() {
        if (sleepingBlockEntityInitialized) {
            globalConfig.getConfigSection(basePath());
            return;
        }
        sleepingBlockEntityInitialized = true;

        enabled = globalConfig.getBoolean(basePath() + ".sleeping-block-entity", enabled);
    }
}
