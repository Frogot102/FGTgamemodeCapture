package com.zov.zovcapture.network;

import com.zov.zovcapture.ZovCaptureMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record CapturePointsSyncPayload(
        List<CapturePointSync> points,
        List<TeamScoreSync> teamScores,
        List<TeamVisualSync> teamVisuals,
        ResourceLocation neutralParticle,
        int pointsToWin,
        boolean gameActive,
        List<BaseZoneSync> baseZones,
        boolean baseZoneParticlesEnabled,
        ResourceLocation baseZoneParticle,
        AirdropStateSync airdrop,
        boolean matchFinished,
        String winningTeam,
        List<PostMatchPlayerStatsSync> postMatchStats,
        boolean matchPreparing,
        int countdownSeconds
) implements CustomPacketPayload {
    public static final Type<CapturePointsSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "points_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CapturePointsSyncPayload> STREAM_CODEC = StreamCodec.of(
            CapturePointsSyncPayload::encode,
            CapturePointsSyncPayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buf, CapturePointsSyncPayload payload) {
        writePoints(buf, payload.points());
        writeScores(buf, payload.teamScores());
        writeVisuals(buf, payload.teamVisuals());
        ResourceLocation.STREAM_CODEC.encode(buf, payload.neutralParticle());
        buf.writeVarInt(payload.pointsToWin());
        buf.writeBoolean(payload.gameActive());
        writeBaseZones(buf, payload.baseZones());
        buf.writeBoolean(payload.baseZoneParticlesEnabled());
        ResourceLocation.STREAM_CODEC.encode(buf, payload.baseZoneParticle());
        AirdropStateSync.STREAM_CODEC.encode(buf, payload.airdrop());
        buf.writeBoolean(payload.matchFinished());
        buf.writeUtf(payload.winningTeam());
        writePostMatchStats(buf, payload.postMatchStats());
        buf.writeBoolean(payload.matchPreparing());
        buf.writeVarInt(payload.countdownSeconds());
    }

    private static void writePostMatchStats(RegistryFriendlyByteBuf buf, List<PostMatchPlayerStatsSync> stats) {
        buf.writeVarInt(stats.size());
        for (PostMatchPlayerStatsSync entry : stats) {
            PostMatchPlayerStatsSync.STREAM_CODEC.encode(buf, entry);
        }
    }

    private static List<PostMatchPlayerStatsSync> readPostMatchStats(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<PostMatchPlayerStatsSync> stats = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stats.add(PostMatchPlayerStatsSync.STREAM_CODEC.decode(buf));
        }
        return stats;
    }

    private static CapturePointsSyncPayload decode(RegistryFriendlyByteBuf buf) {
        return new CapturePointsSyncPayload(
                readPoints(buf),
                readScores(buf),
                readVisuals(buf),
                ResourceLocation.STREAM_CODEC.decode(buf),
                buf.readVarInt(),
                buf.readBoolean(),
                readBaseZones(buf),
                buf.readBoolean(),
                ResourceLocation.STREAM_CODEC.decode(buf),
                AirdropStateSync.STREAM_CODEC.decode(buf),
                buf.readBoolean(),
                buf.readUtf(),
                readPostMatchStats(buf),
                buf.readBoolean(),
                buf.readVarInt()
        );
    }

    private static void writePoints(RegistryFriendlyByteBuf buf, List<CapturePointSync> points) {
        buf.writeVarInt(points.size());
        for (CapturePointSync point : points) {
            CapturePointSync.STREAM_CODEC.encode(buf, point);
        }
    }

    private static List<CapturePointSync> readPoints(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<CapturePointSync> points = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            points.add(CapturePointSync.STREAM_CODEC.decode(buf));
        }
        return points;
    }

    private static void writeScores(RegistryFriendlyByteBuf buf, List<TeamScoreSync> scores) {
        buf.writeVarInt(scores.size());
        for (TeamScoreSync score : scores) {
            TeamScoreSync.STREAM_CODEC.encode(buf, score);
        }
    }

    private static List<TeamScoreSync> readScores(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<TeamScoreSync> scores = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            scores.add(TeamScoreSync.STREAM_CODEC.decode(buf));
        }
        return scores;
    }

    private static void writeVisuals(RegistryFriendlyByteBuf buf, List<TeamVisualSync> visuals) {
        buf.writeVarInt(visuals.size());
        for (TeamVisualSync visual : visuals) {
            TeamVisualSync.STREAM_CODEC.encode(buf, visual);
        }
    }

    private static List<TeamVisualSync> readVisuals(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<TeamVisualSync> visuals = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            visuals.add(TeamVisualSync.STREAM_CODEC.decode(buf));
        }
        return visuals;
    }

    private static void writeBaseZones(RegistryFriendlyByteBuf buf, List<BaseZoneSync> zones) {
        buf.writeVarInt(zones.size());
        for (BaseZoneSync zone : zones) {
            BaseZoneSync.STREAM_CODEC.encode(buf, zone);
        }
    }

    private static List<BaseZoneSync> readBaseZones(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BaseZoneSync> zones = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            zones.add(BaseZoneSync.STREAM_CODEC.decode(buf));
        }
        return zones;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
