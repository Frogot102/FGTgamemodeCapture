package com.zov.zovcapture.network;

import com.zov.zovcapture.ZovCaptureMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestAirdropAdminSyncPayload() implements CustomPacketPayload {
    public static final Type<RequestAirdropAdminSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "request_airdrop_admin"));

    public static final StreamCodec<FriendlyByteBuf, RequestAirdropAdminSyncPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestAirdropAdminSyncPayload());

    public static void handle(RequestAirdropAdminSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.hasPermissions(2)) {
                AirdropAdminNetworking.syncPlayer(player);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
