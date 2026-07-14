package com.zov.zovcapture.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record TeamScoreSync(String team, int score) {
    public static final StreamCodec<FriendlyByteBuf, TeamScoreSync> STREAM_CODEC = StreamCodec.of(
            TeamScoreSync::encode,
            TeamScoreSync::decode
    );

    private static void encode(FriendlyByteBuf buf, TeamScoreSync value) {
        buf.writeUtf(value.team);
        buf.writeVarInt(value.score);
    }

    private static TeamScoreSync decode(FriendlyByteBuf buf) {
        return new TeamScoreSync(buf.readUtf(), buf.readVarInt());
    }
}
