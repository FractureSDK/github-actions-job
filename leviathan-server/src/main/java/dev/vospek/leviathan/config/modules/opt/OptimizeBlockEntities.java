package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class OptimizeBlockEntities extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath();
    }

    public static boolean enabled = true;

    @Override
    public void onLoaded() {
        // Transfer old config
        Boolean optimiseBlockEntities = globalConfig.getBoolean(basePath() + ".optimise-block-entities");
        if (optimiseBlockEntities != null && optimiseBlockEntities) {
            enabled =  true;
        }

        enabled = globalConfig.getBoolean(basePath() + ".optimize-block-entities", enabled);
    }
}
