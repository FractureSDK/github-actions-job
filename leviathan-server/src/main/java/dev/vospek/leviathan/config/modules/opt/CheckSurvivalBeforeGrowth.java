package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class CheckSurvivalBeforeGrowth extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".check-survival-before-growth";
    }

    public static boolean cactusCheckSurvivalBeforeGrowth = false;

    @Override
    public void onLoaded() {
        cactusCheckSurvivalBeforeGrowth = globalConfig.getBoolean(basePath() + ".cactus-check-survival", cactusCheckSurvivalBeforeGrowth,
            globalConfig.pickStringRegionBased("""
                    Check if a cactus can survive before growing.""",
                """
                    在仙人掌生长前检查其是否能够存活。"""));
    }
}
