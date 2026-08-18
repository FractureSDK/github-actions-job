package dev.vospek.leviathan.config.modules.network;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class ChatMessageSignature extends ConfigModule {

    public String basePath() {
        return ConfigCategory.NETWORK.basePath();
    }

    public static boolean enabled = true;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath() + ".chat-message-signature", enabled, globalConfig.pickStringRegionBased("""
                Whether or not enable chat message signature,
                disable will prevent players to report chat messages.
                And also disables the popup when joining a server without
                'secure chat', such as offline-mode servers.""",
            """
                是否启用聊天签名, 禁用后玩家无法进行聊天举报."""));
    }
}
