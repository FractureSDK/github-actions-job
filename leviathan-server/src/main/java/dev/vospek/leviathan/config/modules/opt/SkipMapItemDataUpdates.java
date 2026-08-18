package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class SkipMapItemDataUpdates extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath();
    }

    public static boolean enabled = true;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath() + ".skip-map-item-data-updates-if-map-does-not-have-craftmaprenderer", enabled);
    }
}
