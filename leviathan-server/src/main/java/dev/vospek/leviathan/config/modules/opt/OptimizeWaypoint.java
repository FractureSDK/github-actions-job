package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;
import dev.vospek.leviathan.config.annotations.Experimental;

public class OptimizeWaypoint extends ConfigModule {
    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".optimize-waypoint";
    }

    @Experimental
    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath(), enabled);
    }
}
