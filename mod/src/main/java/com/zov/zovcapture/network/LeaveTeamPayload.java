package com.zov.zovcapture.network;

import com.zov.zovcapture.ZovCaptureMod;
import com.zov.zovcapture.game.TeamJoinManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LeaveTeamPayload() implements CustomPacketPayload {
    public static final Type<LeaveTeamPayload> TYPE =
            new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "leave_team"));

    public static final StreamCodec<FriendlyByteBuf, LeaveTeamPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> new LeaveTeamPayload()
    );

    public static void handle(LeaveTeamPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                TeamJoinManager.leaveTeam(player);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
