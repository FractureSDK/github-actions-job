package dev.vospek.leviathan.protocol;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface LeviathanCustomPayload extends CustomPacketPayload {

    @Override
    Type<? extends LeviathanCustomPayload> type();
}
