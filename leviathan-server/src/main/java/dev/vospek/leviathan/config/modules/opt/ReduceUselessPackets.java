package dev.vospek.leviathan.config.modules.opt;

import dev.vospek.leviathan.config.ConfigModule;
import dev.vospek.leviathan.config.ConfigCategory;

public class ReduceUselessPackets extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".reduce-packets";
    }

    public static boolean reduceUselessEntityMovePackets = false;
    public static boolean filterClientboundSetEntityMotionPacket = false;
    public static boolean disableUselessParticles = false;

    @Override
    public void onLoaded() {
        reduceUselessEntityMovePackets = globalConfig.getBoolean(basePath() + ".reduce-entity-move-packets", reduceUselessEntityMovePackets);
        filterClientboundSetEntityMotionPacket = globalConfig.getBoolean(basePath() + ".reduce-entity-motion-packets", filterClientboundSetEntityMotionPacket);
        disableUselessParticles = globalConfig.getBoolean(basePath() + ".disable-useless-particles", disableUselessParticles);
    }
}
