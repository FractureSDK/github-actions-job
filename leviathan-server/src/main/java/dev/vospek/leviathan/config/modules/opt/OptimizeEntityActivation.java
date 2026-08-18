package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;
import dev.vospek.leviathan.config.annotations.Experimental;

public class OptimizeEntityActivation extends ConfigModule {

    public String getBasePath() {
        return ConfigCategory.PERF.basePath() + ".optimize-entity-activation";
    }

    @Experimental
    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(getBasePath(), enabled);
    }
}
