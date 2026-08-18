package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class OptimizedPoweredRails extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".optimized-powered-rails";
    }

    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath(), enabled,
            globalConfig.pickStringRegionBased(
                """
                    Whether to use optimized powered rails.
                    The implementation is based on RailOptimization made by GitHub@FxMorin""",
                """
                    是否使用铁轨优化。
                    优化实现基于 GitHub@FxMori 的 RailOptimization 模组。"""));
    }
}
