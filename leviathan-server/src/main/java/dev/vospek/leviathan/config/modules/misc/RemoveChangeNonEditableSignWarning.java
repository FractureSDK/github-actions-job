package dev.vospek.leviathan.config.modules.misc;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class RemoveChangeNonEditableSignWarning extends ConfigModule {

    public String basePath() {
        return ConfigCategory.MISC.basePath();
    }

    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath() + ".remove-change-non-editable-sign-warning", enabled,
            globalConfig.pickStringRegionBased(
                "Enable to prevent console spam.",
                "移除修改无法编辑的告示牌时输出的警告."
            ));
    }
}
