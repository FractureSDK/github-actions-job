package dev.vospek.leviathan.config.modules.network;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class AlternativeJoin extends ConfigModule {

    public String basePath() {
        return ConfigCategory.NETWORK.basePath();
    }

    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath() + ".async-switch-state", enabled, globalConfig.pickStringRegionBased(
            "Async switch connection state.",
            "异步切换连接状态."));
    }
}
