package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;
import dev.vospek.leviathan.config.annotations.Experimental;

public class OptimizeNoActionTime extends ConfigModule {
    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".optimize-no-action-time";
    }

    @Experimental
    public static boolean disableLightCheck = false;

    @Override
    public void onLoaded() {
        disableLightCheck = globalConfig.getBoolean(basePath() + ".disable-light-check", disableLightCheck);
    }
}
