package com.zov.zovcapture.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public record BaseZoneSync(
        String id,
        String displayName,
        BlockPos center,
        int radius,
        ResourceLocation dimension,
        @Nullable String team
) {
    public static final StreamCodec<FriendlyByteBuf, BaseZoneSync> STREAM_CODEC = StreamCodec.of(
            BaseZoneSync::encode,
            BaseZoneSync::decode
    );

    private static void encode(FriendlyByteBuf buf, BaseZoneSync zone) {
        buf.writeUtf(zone.id);
        buf.writeUtf(zone.displayName);
        buf.writeBlockPos(zone.center);
        buf.writeVarInt(zone.radius);
        buf.writeResourceLocation(zone.dimension);
        buf.writeUtf(zone.team == null ? "" : zone.team);
    }

    private static BaseZoneSync decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String displayName = buf.readUtf();
        BlockPos center = buf.readBlockPos();
        int radius = buf.readVarInt();
        ResourceLocation dimension = buf.readResourceLocation();
        String team = buf.readUtf();
        return new BaseZoneSync(
                id,
                displayName,
                center,
                radius,
                dimension,
                team.isEmpty() ? null : team
        );
    }
}
