package dev.vospek.leviathan.config.modules.gameplay;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class LocatorBarRange extends ConfigModule {

    public String basePath() {
        return ConfigCategory.GAMEPLAY.basePath();
    }

    public static double maxDistance = 0.0D;

    @Override
    public void onLoaded() {
        maxDistance = globalConfig.getDouble(basePath() + ".locator-bar-max-distance", maxDistance, globalConfig.pickStringRegionBased("""
                Maximum distance in blocks at which players show up on each other's
                locator bar.
                Set to 0 to keep vanilla behaviour.""",
            """
                玩家在彼此定位栏上显示的最大距离 (单位: 方块).
                设置为 0 保持原版行为."""));
    }
}
