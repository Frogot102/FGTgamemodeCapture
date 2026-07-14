package com.zov.zovcapture.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public record AirdropStateSync(
        boolean enabled,
        int secondsUntilNext,
        boolean active,
        String displayName,
        BlockPos cratePos,
        ResourceLocation dimension,
        int radius,
        float captureProgress,
        String capturingPlayer,
        ResourceLocation particle
) {
    public static final AirdropStateSync EMPTY = new AirdropStateSync(
            false, -1, false, "", BlockPos.ZERO, ResourceLocation.withDefaultNamespace("overworld"),
            0, 0.0F, "", ResourceLocation.withDefaultNamespace("dust")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AirdropStateSync> STREAM_CODEC = StreamCodec.of(
            AirdropStateSync::encode,
            AirdropStateSync::decode
    );

    private static void encode(RegistryFriendlyByteBuf buf, AirdropStateSync state) {
        buf.writeBoolean(state.enabled);
        buf.writeVarInt(state.secondsUntilNext);
        buf.writeBoolean(state.active);
        buf.writeUtf(state.displayName);
        buf.writeBlockPos(state.cratePos);
        ResourceLocation.STREAM_CODEC.encode(buf, state.dimension);
        buf.writeVarInt(state.radius);
        buf.writeFloat(state.captureProgress);
        buf.writeUtf(state.capturingPlayer);
        ResourceLocation.STREAM_CODEC.encode(buf, state.particle);
    }

    private static AirdropStateSync decode(RegistryFriendlyByteBuf buf) {
        return new AirdropStateSync(
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readBlockPos(),
                ResourceLocation.STREAM_CODEC.decode(buf),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readUtf(),
                ResourceLocation.STREAM_CODEC.decode(buf)
        );
    }
}
