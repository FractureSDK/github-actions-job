package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;
import dev.vospek.leviathan.config.annotations.Experimental;

public class OptimizeRandomTick extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".optimize-random-tick";
    }

    @Experimental
    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        Boolean old = globalConfig.getBoolean(ConfigCategory.PERF.basePath() + ".optimise-random-tick");
        if (old != null && old) {
            enabled = globalConfig.getBoolean(basePath(), true);
            return;
        }

        enabled = globalConfig.getBoolean(basePath(), enabled);
    }
}
