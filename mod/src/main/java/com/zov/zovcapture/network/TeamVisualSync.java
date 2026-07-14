package com.zov.zovcapture.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record TeamVisualSync(String team, String particle, String bossBarColor) {
    public static final StreamCodec<FriendlyByteBuf, TeamVisualSync> STREAM_CODEC = StreamCodec.of(
            TeamVisualSync::encode,
            TeamVisualSync::decode
    );

    private static void encode(FriendlyByteBuf buf, TeamVisualSync value) {
        buf.writeUtf(value.team);
        buf.writeUtf(value.particle);
        buf.writeUtf(value.bossBarColor);
    }

    private static TeamVisualSync decode(FriendlyByteBuf buf) {
        return new TeamVisualSync(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }
}
