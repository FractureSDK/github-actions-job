package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;
import dev.vospek.leviathan.config.annotations.Experimental;

public class DespawnTime extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".despawn-time";
    }

    @Experimental
    public static boolean proactiveWeakLoading = false;

    @Override
    public void onLoaded() {
        proactiveWeakLoading = globalConfig.getBoolean(basePath() + ".proactive-weak-loading-despawn", proactiveWeakLoading,
            globalConfig.pickStringRegionBased("""
                    Proactive despawn check for weak-loaded entities.
                    This is an experimental feature.""",
                """
                    启用主动弱加载实体消失检查，
                    这是一个实验性功能。"""));
    }
}
