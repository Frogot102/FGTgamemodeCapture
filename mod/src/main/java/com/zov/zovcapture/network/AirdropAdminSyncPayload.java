package com.zov.zovcapture.network;

import com.zov.zovcapture.ZovCaptureMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record AirdropAdminSyncPayload(
        boolean enabled,
        int intervalSeconds,
        int captureSeconds,
        ResourceLocation particle,
        List<AirdropSpawnPointSync> spawnPoints,
        List<AirdropLootSync> loot
) implements CustomPacketPayload {
    public static final Type<AirdropAdminSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "airdrop_admin_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AirdropAdminSyncPayload> STREAM_CODEC = StreamCodec.of(
            AirdropAdminSyncPayload::encode,
            AirdropAdminSyncPayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buf, AirdropAdminSyncPayload payload) {
        buf.writeBoolean(payload.enabled);
        buf.writeVarInt(payload.intervalSeconds);
        buf.writeVarInt(payload.captureSeconds);
        ResourceLocation.STREAM_CODEC.encode(buf, payload.particle);
        buf.writeVarInt(payload.spawnPoints.size());
        for (AirdropSpawnPointSync point : payload.spawnPoints) {
            AirdropSpawnPointSync.STREAM_CODEC.encode(buf, point);
        }
        buf.writeVarInt(payload.loot.size());
        for (AirdropLootSync entry : payload.loot) {
            AirdropLootSync.STREAM_CODEC.encode(buf, entry);
        }
    }

    private static AirdropAdminSyncPayload decode(RegistryFriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean();
        int interval = buf.readVarInt();
        int capture = buf.readVarInt();
        ResourceLocation particle = ResourceLocation.STREAM_CODEC.decode(buf);
        int pointCount = buf.readVarInt();
        List<AirdropSpawnPointSync> points = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            points.add(AirdropSpawnPointSync.STREAM_CODEC.decode(buf));
        }
        int lootCount = buf.readVarInt();
        List<AirdropLootSync> loot = new ArrayList<>(lootCount);
        for (int i = 0; i < lootCount; i++) {
            loot.add(AirdropLootSync.STREAM_CODEC.decode(buf));
        }
        return new AirdropAdminSyncPayload(enabled, interval, capture, particle, points, loot);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record AirdropSpawnPointSync(String id, String displayName, BlockPos center, int radius, ResourceLocation dimension) {
        public static final StreamCodec<RegistryFriendlyByteBuf, AirdropSpawnPointSync> STREAM_CODEC = StreamCodec.of(
                AirdropSpawnPointSync::encode,
                AirdropSpawnPointSync::decode
        );

        private static void encode(RegistryFriendlyByteBuf buf, AirdropSpawnPointSync point) {
            buf.writeUtf(point.id);
            buf.writeUtf(point.displayName);
            buf.writeBlockPos(point.center);
            buf.writeVarInt(point.radius);
            ResourceLocation.STREAM_CODEC.encode(buf, point.dimension);
        }

        private static AirdropSpawnPointSync decode(RegistryFriendlyByteBuf buf) {
            return new AirdropSpawnPointSync(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readBlockPos(),
                    buf.readVarInt(),
                    ResourceLocation.STREAM_CODEC.decode(buf)
            );
        }
    }

    public record AirdropLootSync(
            String id,
            String displayName,
            int weight,
            String type,
            String payload,
            int count,
            List<String> bundleLines
    ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, AirdropLootSync> STREAM_CODEC = StreamCodec.of(
                AirdropLootSync::encode,
                AirdropLootSync::decode
        );

        private static void encode(RegistryFriendlyByteBuf buf, AirdropLootSync entry) {
            buf.writeUtf(entry.id);
            buf.writeUtf(entry.displayName);
            buf.writeVarInt(entry.weight);
            buf.writeUtf(entry.type);
            buf.writeUtf(entry.payload);
            buf.writeVarInt(entry.count);
            buf.writeVarInt(entry.bundleLines.size());
            for (String line : entry.bundleLines) {
                buf.writeUtf(line);
            }
        }

        private static AirdropLootSync decode(RegistryFriendlyByteBuf buf) {
            String id = buf.readUtf();
            String displayName = buf.readUtf();
            int weight = buf.readVarInt();
            String type = buf.readUtf();
            String payload = buf.readUtf();
            int count = buf.readVarInt();
            int lineCount = buf.readVarInt();
            List<String> lines = new ArrayList<>(lineCount);
            for (int i = 0; i < lineCount; i++) {
                lines.add(buf.readUtf());
            }
            return new AirdropLootSync(id, displayName, weight, type, payload, count, lines);
        }
    }
}
