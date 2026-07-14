package com.zov.zovcapture.network;

import com.zov.zovcapture.airdrop.ActiveAirdrop;
import com.zov.zovcapture.airdrop.AirdropManager;
import com.zov.zovcapture.client.ClientCaptureData;
import com.zov.zovcapture.game.BaseZone;
import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.game.CapturePoint;
import com.zov.zovcapture.game.MatchStatsTracker;
import com.zov.zovcapture.game.TeamVisualSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CaptureNetworking {
    private static final String VERSION = "12";

    private CaptureNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(
                CapturePointsSyncPayload.TYPE,
                CapturePointsSyncPayload.STREAM_CODEC,
                CaptureNetworking::handleClientSync
        );
        registrar.playToClient(
                EconomySyncPayload.TYPE,
                EconomySyncPayload.STREAM_CODEC,
                EconomyNetworking::handleClientSync
        );
        registrar.playToClient(
                AirdropAdminSyncPayload.TYPE,
                AirdropAdminSyncPayload.STREAM_CODEC,
                AirdropAdminNetworking::handleClientSync
        );
        registrar.playToServer(
                ShopBuyPayload.TYPE,
                ShopBuyPayload.STREAM_CODEC,
                ShopBuyPayload::handle
        );
        registrar.playToClient(
                OpenMenuPayload.TYPE,
                OpenMenuPayload.STREAM_CODEC,
                OpenMenuPayload::handle
        );
        registrar.playToClient(
                OpenAirdropAdminPayload.TYPE,
                OpenAirdropAdminPayload.STREAM_CODEC,
                OpenAirdropAdminPayload::handle
        );
        registrar.playToServer(
                RequestEconomySyncPayload.TYPE,
                RequestEconomySyncPayload.STREAM_CODEC,
                RequestEconomySyncPayload::handle
        );
        registrar.playToServer(
                RequestAirdropAdminSyncPayload.TYPE,
                RequestAirdropAdminSyncPayload.STREAM_CODEC,
                RequestAirdropAdminSyncPayload::handle
        );
        registrar.playToServer(
                JoinTeamPayload.TYPE,
                JoinTeamPayload.STREAM_CODEC,
                JoinTeamPayload::handle
        );
        registrar.playToServer(
                SelectClassPayload.TYPE,
                SelectClassPayload.STREAM_CODEC,
                SelectClassPayload::handle
        );
        registrar.playToServer(
                LeaveTeamPayload.TYPE,
                LeaveTeamPayload.STREAM_CODEC,
                LeaveTeamPayload::handle
        );
        registrar.playToServer(
                ClearClassPayload.TYPE,
                ClearClassPayload.STREAM_CODEC,
                ClearClassPayload::handle
        );
    }

    private static void handleClientSync(CapturePointsSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCaptureData.update(
                payload.points(),
                payload.teamScores(),
                payload.teamVisuals(),
                payload.neutralParticle(),
                payload.pointsToWin(),
                payload.gameActive(),
                payload.baseZones(),
                payload.baseZoneParticlesEnabled(),
                payload.baseZoneParticle(),
                payload.airdrop(),
                payload.matchFinished(),
                payload.winningTeam(),
                payload.postMatchStats(),
                payload.matchPreparing(),
                payload.countdownSeconds()
        ));
    }

    public static void syncToAll(MinecraftServer server) {
        CapturePointsSyncPayload payload = createPayload(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(payload);
        }
        EconomyNetworking.syncAll(server);
    }

    public static void syncToPlayer(ServerPlayer player) {
        player.connection.send(createPayload(player.server));
        EconomyNetworking.syncPlayer(player);
    }

    private static CapturePointsSyncPayload createPayload(MinecraftServer server) {
        CaptureGameData data = CaptureGameData.get(server.overworld());
        List<CapturePointSync> points = new ArrayList<>();
        for (CapturePoint point : data.points().values()) {
            points.add(new CapturePointSync(
                    point.id(),
                    point.displayName(),
                    point.center(),
                    point.radius(),
                    point.dimension().location(),
                    point.progress(),
                    point.ownerTeam(),
                    point.capturingTeam()
            ));
        }

        List<BaseZoneSync> baseZones = new ArrayList<>();
        for (BaseZone zone : data.baseZones().values()) {
            baseZones.add(new BaseZoneSync(
                    zone.id(),
                    zone.displayName(),
                    zone.center(),
                    zone.radius(),
                    zone.dimension().location(),
                    zone.team()
            ));
        }

        List<TeamScoreSync> scores = new ArrayList<>();
        var scoreboard = server.getScoreboard();
        for (var team : scoreboard.getPlayerTeams()) {
            scores.add(new TeamScoreSync(team.getName(), data.getScore(team.getName())));
        }
        for (Map.Entry<String, Integer> entry : data.teamScores().entrySet()) {
            if (scoreboard.getPlayerTeam(entry.getKey()) == null) {
                scores.add(new TeamScoreSync(entry.getKey(), entry.getValue()));
            }
        }

        List<TeamVisualSync> visuals = new ArrayList<>();
        for (Map.Entry<String, TeamVisualSettings> entry : data.teamVisuals().entrySet()) {
            TeamVisualSettings settings = entry.getValue();
            visuals.add(new TeamVisualSync(
                    entry.getKey(),
                    settings.particle().map(ResourceLocation::toString).orElse(""),
                    settings.bossBarColor().map(color -> color.getName()).orElse("")
            ));
        }

        long gameTime = server.overworld().getGameTime();
        return new CapturePointsSyncPayload(
                points,
                scores,
                visuals,
                data.neutralParticle(),
                data.pointsToWin(),
                data.gameActive() && !data.matchFinished(),
                baseZones,
                data.baseZoneParticlesEnabled(),
                data.baseZoneParticle(),
                createAirdropState(server, data),
                data.matchFinished(),
                data.winningTeam() != null ? data.winningTeam() : "",
                data.matchFinished() ? MatchStatsTracker.export(data) : List.of(),
                data.isCountdownPending(),
                data.countdownSecondsRemaining(gameTime)
        );
    }

    private static AirdropStateSync createAirdropState(MinecraftServer server, CaptureGameData data) {
        long gameTime = server.overworld().getGameTime();
        ActiveAirdrop active = data.activeAirdrop();
        if (active == null) {
            return new AirdropStateSync(
                    data.airdropEnabled(),
                    AirdropManager.secondsUntilNext(data, gameTime),
                    false,
                    "",
                    net.minecraft.core.BlockPos.ZERO,
                    server.overworld().dimension().location(),
                    0,
                    0.0F,
                    "",
                    data.airdropParticle()
            );
        }

        String capturingPlayer = "";
        if (active.capturingPlayer() != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(active.capturingPlayer());
            if (player != null) {
                capturingPlayer = player.getGameProfile().getName();
            }
        }

        return new AirdropStateSync(
                data.airdropEnabled(),
                0,
                true,
                active.displayName(),
                active.cratePos(),
                active.dimension().location(),
                active.radius(),
                active.captureProgress(),
                capturingPlayer,
                data.airdropParticle()
        );
    }
}
