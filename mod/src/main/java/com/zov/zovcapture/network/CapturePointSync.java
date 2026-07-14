package com.zov.zovcapture.network;

import com.zov.zovcapture.ZovCaptureMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CapturePointSync(
        String id,
        String displayName,
        BlockPos center,
        int radius,
        ResourceLocation dimension,
        float progress,
        String ownerTeam,
        String capturingTeam
) {
    public static final StreamCodec<FriendlyByteBuf, CapturePointSync> STREAM_CODEC = StreamCodec.of(
            CapturePointSync::encode,
            CapturePointSync::decode
    );

    private static void encode(FriendlyByteBuf buf, CapturePointSync point) {
        buf.writeUtf(point.id);
        buf.writeUtf(point.displayName);
        buf.writeBlockPos(point.center);
        buf.writeVarInt(point.radius);
        buf.writeResourceLocation(point.dimension);
        buf.writeFloat(point.progress);
        buf.writeUtf(point.ownerTeam == null ? "" : point.ownerTeam);
        buf.writeUtf(point.capturingTeam == null ? "" : point.capturingTeam);
    }

    private static CapturePointSync decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String displayName = buf.readUtf();
        BlockPos center = buf.readBlockPos();
        int radius = buf.readVarInt();
        ResourceLocation dimension = buf.readResourceLocation();
        float progress = buf.readFloat();
        String ownerTeam = buf.readUtf();
        String capturingTeam = buf.readUtf();
        return new CapturePointSync(
                id,
                displayName,
                center,
                radius,
                dimension,
                progress,
                ownerTeam.isEmpty() ? null : ownerTeam,
                capturingTeam.isEmpty() ? null : capturingTeam
        );
    }
}
