package dev.vospek.leviathan.config.modules.async;

import dev.vospek.leviathan.async.tracker.AsyncTracker;
import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;
import dev.vospek.leviathan.config.LeviathanConfig;
import dev.vospek.leviathan.config.annotations.Experimental;

public class MultithreadedTracker extends ConfigModule {

    public String basePath() {
        return ConfigCategory.ASYNC.basePath() + ".async-entity-tracker";
    }

    @Experimental
    public static boolean enabled = false;
    public static int threads = 0;
    private static boolean asyncMultithreadedTrackerInitialized;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(), """
                ** Experimental Feature **
                Make entity tracking asynchronously, can improve performance significantly,
                especially in some massive entities in small area situations.""", """
                ** 实验性功能 **
                异步实体跟踪,
                在实体数量多且密集的情况下效果明显.""");

        if (asyncMultithreadedTrackerInitialized) {
            globalConfig.getConfigSection(basePath());
            return;
        }
        asyncMultithreadedTrackerInitialized = true;

        enabled = globalConfig.getBoolean(basePath() + ".enabled", false);
        threads = globalConfig.getInt(basePath() + ".threads", 0);

        if (threads <= 0) {
            threads = Math.min(Runtime.getRuntime().availableProcessors(), 4);
        }
        threads = Math.max(threads, 1);

        if (enabled) {
            LeviathanConfig.LOGGER.info("Using {} threads for Async Entity Tracker", threads);
            AsyncTracker.init();
        }
    }
}
