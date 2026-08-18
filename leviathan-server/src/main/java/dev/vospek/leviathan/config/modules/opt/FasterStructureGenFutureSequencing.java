package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class FasterStructureGenFutureSequencing extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath();
    }

    public static boolean enabled = true;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath() + ".faster-structure-gen-future-sequencing", enabled,
            globalConfig.pickStringRegionBased(
                "May cause the inconsistent order of future compose tasks.",
                "更快的结构生成任务分段."));
    }
}
