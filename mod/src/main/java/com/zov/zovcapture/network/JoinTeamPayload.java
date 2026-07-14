package com.zov.zovcapture.network;

import com.zov.zovcapture.ZovCaptureMod;
import com.zov.zovcapture.game.TeamJoinManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JoinTeamPayload(String teamName) implements CustomPacketPayload {
    public static final Type<JoinTeamPayload> TYPE =
            new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "join_team"));

    public static final StreamCodec<FriendlyByteBuf, JoinTeamPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.teamName()),
            buf -> new JoinTeamPayload(buf.readUtf())
    );

    public static void handle(JoinTeamPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                TeamJoinManager.joinTeam(player, payload.teamName());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
