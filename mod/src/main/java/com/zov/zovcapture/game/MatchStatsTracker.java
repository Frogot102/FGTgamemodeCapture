package com.zov.zovcapture.game;

import com.zov.zovcapture.network.PostMatchPlayerStatsSync;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MatchStatsTracker {
    private MatchStatsTracker() {
    }

    public static PlayerMatchStats getOrCreate(CaptureGameData data, ServerPlayer player) {
        return data.getOrCreateMatchStats(player.getUUID(), player.getGameProfile().getName(), teamName(player));
    }

    public static void recordKill(CaptureGameData data, ServerPlayer killer, ServerPlayer victim) {
        getOrCreate(data, killer).addKill();
        getOrCreate(data, victim).addDeath();
        data.setDirty();
    }

    public static void recordAirdrop(CaptureGameData data, ServerPlayer player) {
        getOrCreate(data, player).addAirdrop();
        data.setDirty();
    }

    public static void recordMoneyEarned(CaptureGameData data, UUID playerId, String playerName, @javax.annotation.Nullable String team, int amount) {
        if (amount <= 0) {
            return;
        }
        PlayerMatchStats stats = data.getOrCreateMatchStats(playerId, playerName, team != null ? team : "");
        stats.addMoneyEarned(amount);
        data.setDirty();
    }

    public static void recordPointCapture(CaptureGameData data, MinecraftServer server, CapturePoint point, String teamName) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getTeam() == null || !teamName.equals(player.getTeam().getName())) {
                continue;
            }
            if (!isInPointZone(player, point)) {
                continue;
            }
            getOrCreate(data, player).addCapture();
        }
        data.setDirty();
    }

    public static List<PostMatchPlayerStatsSync> export(CaptureGameData data) {
        List<PostMatchPlayerStatsSync> exported = new ArrayList<>();
        for (Map.Entry<UUID, PlayerMatchStats> entry : data.matchStatsView().entrySet()) {
            PlayerMatchStats stats = entry.getValue();
            exported.add(new PostMatchPlayerStatsSync(
                    stats.playerName(),
                    stats.teamName(),
                    stats.kills(),
                    stats.deaths(),
                    stats.captures(),
                    stats.airdrops(),
                    stats.moneyEarned()
            ));
        }
        exported.sort(Comparator.comparingInt(PostMatchPlayerStatsSync::kills).reversed()
                .thenComparing(PostMatchPlayerStatsSync::playerName));
        return exported;
    }

    private static boolean isInPointZone(ServerPlayer player, CapturePoint point) {
        if (!CaptureParticipation.canParticipateInCapture(player)) {
            return false;
        }
        if (!player.level().dimension().equals(point.dimension())) {
            return false;
        }
        return point.contains(player.blockPosition());
    }

    private static String teamName(ServerPlayer player) {
        Team team = player.getTeam();
        return team != null ? team.getName() : "";
    }
}
