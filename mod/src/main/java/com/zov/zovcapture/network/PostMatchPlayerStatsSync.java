package com.zov.zovcapture.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record PostMatchPlayerStatsSync(
        String playerName,
        String teamName,
        int kills,
        int deaths,
        int captures,
        int airdrops,
        int moneyEarned
) {
    public static final StreamCodec<FriendlyByteBuf, PostMatchPlayerStatsSync> STREAM_CODEC = StreamCodec.of(
            PostMatchPlayerStatsSync::encode,
            PostMatchPlayerStatsSync::decode
    );

    private static void encode(FriendlyByteBuf buf, PostMatchPlayerStatsSync stats) {
        buf.writeUtf(stats.playerName);
        buf.writeUtf(stats.teamName);
        buf.writeVarInt(stats.kills);
        buf.writeVarInt(stats.deaths);
        buf.writeVarInt(stats.captures);
        buf.writeVarInt(stats.airdrops);
        buf.writeVarInt(stats.moneyEarned);
    }

    private static PostMatchPlayerStatsSync decode(FriendlyByteBuf buf) {
        return new PostMatchPlayerStatsSync(
                buf.readUtf(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }
}
