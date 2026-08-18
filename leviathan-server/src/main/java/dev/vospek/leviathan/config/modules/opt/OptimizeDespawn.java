package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;
import dev.vospek.leviathan.config.annotations.Experimental;
import dev.vospek.leviathan.util.LeviathanConstants;

public class OptimizeDespawn extends ConfigModule {
    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".optimize-mob-despawn";
    }

    @Experimental
    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath(), enabled);
        if (enabled) {
            if (!LeviathanConstants.ENABLE_FMA) {
                LOGGER.info("NOTE: Recommend enabling FMA to work with optimize-mob-despawn.");
            }
        }
    }
}
