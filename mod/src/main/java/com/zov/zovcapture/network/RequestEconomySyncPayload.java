package com.zov.zovcapture.network;

import com.zov.zovcapture.ZovCaptureMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestEconomySyncPayload() implements CustomPacketPayload {
    public static final Type<RequestEconomySyncPayload> TYPE =
            new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "request_economy_sync"));

    public static final StreamCodec<FriendlyByteBuf, RequestEconomySyncPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestEconomySyncPayload());

    public static void handle(RequestEconomySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                EconomyNetworking.syncPlayer(player);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
