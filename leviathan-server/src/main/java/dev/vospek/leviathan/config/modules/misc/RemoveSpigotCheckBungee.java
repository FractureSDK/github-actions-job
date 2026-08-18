package dev.vospek.leviathan.config.modules.misc;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class RemoveSpigotCheckBungee extends ConfigModule {

    public String basePath() {
        return ConfigCategory.MISC.basePath() + ".remove-spigot-check-bungee-config";
    }

    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath(), enabled, globalConfig.pickStringRegionBased("""
                Enable player enter backend server through proxy
                without backend server enabling its bungee mode.""",
            """
                使服务器无需打开 bungee 模式即可让玩家加入后端服务器."""));
    }
}
