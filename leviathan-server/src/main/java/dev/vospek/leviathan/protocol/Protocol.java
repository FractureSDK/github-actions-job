package dev.vospek.leviathan.protocol;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

interface Protocol {

    String namespace();

    List<Protocols.TypeAndCodec<FriendlyByteBuf, ? extends LeviathanCustomPayload>> c2s();

    List<Protocols.TypeAndCodec<FriendlyByteBuf, ? extends LeviathanCustomPayload>> s2c();

    void tickServer(MinecraftServer server);

    void tickPlayer(ServerPlayer player);

    void disconnected(ServerPlayer conn);

    void handle(ServerPlayer player, LeviathanCustomPayload payload);
}
