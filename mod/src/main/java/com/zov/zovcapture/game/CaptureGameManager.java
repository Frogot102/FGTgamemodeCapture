package com.zov.zovcapture.game;

import com.zov.zovcapture.ZovCaptureConfig;
import com.zov.zovcapture.airdrop.AirdropManager;
import com.zov.zovcapture.economy.EconomyManager;
import com.zov.zovcapture.network.CaptureNetworking;
import com.zov.zovcapture.shop.BalanceShopPreset;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CaptureGameManager {
    private static final Map<String, ServerBossEvent> BOSS_BARS = new HashMap<>();
    private static int tickCounter = 0;
    @Nullable
    private static MinecraftServer server;

    private CaptureGameManager() {
    }

    public static void onServerStarting(MinecraftServer minecraftServer) {
        server = minecraftServer;
        rebuildBossBars(minecraftServer);
        CaptureGameData data = CaptureGameData.get(minecraftServer.overworld());
        if (data.shopOffers().isEmpty()) {
            BalanceShopPreset.applyTo(minecraftServer, data);
            CaptureNetworking.syncToAll(minecraftServer);
        }
        TeamFriendlyFireRules.disableForAllTeams(minecraftServer);
        long gameTime = minecraftServer.overworld().getGameTime();
        if (data.isCountdownPending()) {
            MatchCountdownBossBar.show(
                    minecraftServer,
                    MatchStartManager.COUNTDOWN_SECONDS,
                    data.countdownSecondsRemaining(gameTime)
            );
        } else if (data.countdownEndTick() > 0L && gameTime >= data.countdownEndTick() && !data.gameActive()) {
            MatchStartManager.completePreparation(minecraftServer, data);
        }
    }

    public static void onServerStopping() {
        BOSS_BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BOSS_BARS.clear();
        MatchCountdownBossBar.clear();
        tickCounter = 0;
        server = null;
    }

    public static void onServerTick(MinecraftServer minecraftServer) {
        tickCounter++;
        refreshBossBarViewers(minecraftServer);

        CaptureGameData data = CaptureGameData.get(minecraftServer.overworld());
        long gameTime = minecraftServer.overworld().getGameTime();
        if (data.isCountdownPending()) {
            MatchStartManager.tickCountdown(minecraftServer, data);
            if (data.isCountdownPending()) {
                MatchPreparationGuard.enforce(minecraftServer, data);
            }
            return;
        }

        if (!data.gameActive() || data.matchFinished()) {
            return;
        }

        boolean stateChanged = false;

        if (tickCounter % 20 == 0) {
            stateChanged |= awardHoldScores(minecraftServer, data);
        }

        EconomyManager.awardHoldRewards(minecraftServer, data);

        if (tickCounter % ZovCaptureConfig.TICK_INTERVAL.get() == 0) {
            for (CapturePoint point : data.points().values()) {
                ServerLevel level = minecraftServer.getLevel(point.dimension());
                if (level == null) {
                    continue;
                }
                tickPointCapture(minecraftServer, point, level);
                updateBossBar(minecraftServer, data, point);
            }
            data.setDirty();
            stateChanged = true;
        }

        if (stateChanged) {
            CaptureNetworking.syncToAll(minecraftServer);
        }

        AirdropManager.onServerTick(minecraftServer, data);
    }

    private static void tickPointCapture(MinecraftServer minecraftServer, CapturePoint point, ServerLevel level) {
        float captureSpeed = ZovCaptureConfig.CAPTURE_SPEED.get().floatValue();
        float decaySpeed = ZovCaptureConfig.DECAY_SPEED.get().floatValue();

        Map<String, Integer> teamCounts = countTeamsInZone(level, point);
        Dominance dominance = resolveDominance(teamCounts);

        String owner = point.ownerTeam();
        float progress = point.progress();

        if (owner != null && progress >= 1.0F) {
            point.setProgress(1.0F);

            if (dominance.dominantTeam() != null
                    && !dominance.dominantTeam().equals(owner)
                    && !dominance.contested()) {
                point.setCapturingTeam(null);
                point.setProgress(Math.max(0.0F, progress - decaySpeed * dominance.dominantCount()));
                if (point.progress() <= 0.0F) {
                    point.setProgress(0.0F);
                    point.setOwnerTeam(null);
                }
            } else {
                point.setCapturingTeam(owner);
            }
            return;
        }

        if (dominance.dominantTeam() != null && !dominance.contested()) {
            String dominant = dominance.dominantTeam();

            if (owner != null && !owner.equals(dominant)) {
                point.setCapturingTeam(null);
                point.setProgress(Math.max(0.0F, progress - decaySpeed * dominance.dominantCount()));
                if (point.progress() <= 0.0F) {
                    point.setProgress(0.0F);
                    point.setOwnerTeam(null);
                }
                return;
            }

            point.setCapturingTeam(dominant);
            point.setProgress(Math.min(1.0F, progress + captureSpeed * dominance.dominantCount()));
            if (point.progress() >= 1.0F) {
                point.setProgress(1.0F);
                point.setOwnerTeam(dominant);
                point.setCapturingTeam(dominant);
                announceCapture(minecraftServer, point, dominant);
            }
            return;
        }

        point.setCapturingTeam(null);
        if (owner == null && progress > 0.0F) {
            point.setProgress(Math.max(0.0F, progress - decaySpeed));
        } else if (owner != null && progress < 1.0F) {
            point.setProgress(Math.max(0.0F, progress - decaySpeed));
            if (point.progress() <= 0.0F) {
                point.setProgress(0.0F);
                point.setOwnerTeam(null);
            }
        }
    }

    private static void announceCapture(MinecraftServer minecraftServer, CapturePoint point, String teamName) {
        Scoreboard scoreboard = minecraftServer.getScoreboard();
        Team team = scoreboard.getPlayerTeam(teamName);
        Component teamNameComponent = TeamColors.teamDisplayName(team, teamName);

        minecraftServer.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.zovcapture.point_captured", teamNameComponent, point.displayName()),
                false
        );
        CaptureGameData data = CaptureGameData.get(minecraftServer.overworld());
        MatchStatsTracker.recordPointCapture(data, minecraftServer, point, teamName);
    }

    private static boolean awardHoldScores(MinecraftServer minecraftServer, CaptureGameData data) {
        int pointsPerPoint = ZovCaptureConfig.SCORE_PER_CAPTURED_POINT.get();
        if (pointsPerPoint <= 0) {
            return false;
        }

        Map<String, Integer> gained = new HashMap<>();
        for (CapturePoint point : data.points().values()) {
            if (point.ownerTeam() != null && point.progress() >= 1.0F) {
                gained.merge(point.ownerTeam(), pointsPerPoint, Integer::sum);
            }
        }

        if (gained.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, Integer> entry : gained.entrySet()) {
            data.addScore(entry.getKey(), entry.getValue());
            if (checkWin(minecraftServer, data, entry.getKey())) {
                return true;
            }
        }

        data.setDirty();
        return true;
    }

    private static boolean checkWin(MinecraftServer minecraftServer, CaptureGameData data, String teamName) {
        if (data.getScore(teamName) >= data.pointsToWin()) {
            finishMatch(minecraftServer, data, teamName);
            CaptureNetworking.syncToAll(minecraftServer);
            return true;
        }
        return false;
    }

    private static void finishMatch(MinecraftServer minecraftServer, CaptureGameData data, String winningTeamName) {
        data.finishMatch(winningTeamName);
        MatchCountdownBossBar.clear();
        MatchLifecycle.onMatchEnd(minecraftServer, true);
        Scoreboard scoreboard = minecraftServer.getScoreboard();
        Team winningTeam = scoreboard.getPlayerTeam(winningTeamName);
        Component winnerName = TeamColors.teamDisplayName(winningTeam, winningTeamName);

        for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
            Team playerTeam = player.getTeam();
            boolean isWinner = playerTeam != null && winningTeamName.equals(playerTeam.getName());
            if (isWinner) {
                player.sendSystemMessage(Component.translatable("message.zovcapture.victory", winnerName));
            } else {
                player.sendSystemMessage(Component.translatable("message.zovcapture.defeat", winnerName));
            }
        }
    }

    private static Map<String, Integer> countTeamsInZone(ServerLevel level, CapturePoint point) {
        Map<String, Integer> counts = new HashMap<>();
        AABB box = new AABB(
                point.center().getX() - point.radius(),
                point.center().getY() - point.radius(),
                point.center().getZ() - point.radius(),
                point.center().getX() + point.radius() + 1.0,
                point.center().getY() + point.radius() + 1.0,
                point.center().getZ() + point.radius() + 1.0
        );

        for (Player player : level.getEntitiesOfClass(Player.class, box, Player::isAlive)) {
            if (!CaptureParticipation.canParticipateInCapture(player)) {
                continue;
            }
            double dx = player.getX() - (point.center().getX() + 0.5);
            double dy = player.getY() - (point.center().getY() + 0.5);
            double dz = player.getZ() - (point.center().getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz > point.radius() * point.radius()) {
                continue;
            }

            Team team = TeamColors.getPlayerTeam(player);
            if (team == null) {
                continue;
            }
            counts.merge(team.getName(), 1, Integer::sum);
        }
        return counts;
    }

    private static Dominance resolveDominance(Map<String, Integer> teamCounts) {
        String dominant = null;
        int dominantCount = 0;
        int secondCount = 0;

        for (Map.Entry<String, Integer> entry : teamCounts.entrySet()) {
            int count = entry.getValue();
            if (count > dominantCount) {
                secondCount = dominantCount;
                dominantCount = count;
                dominant = entry.getKey();
            } else if (count > secondCount) {
                secondCount = count;
            }
        }

        boolean contested = dominantCount > 0 && dominantCount == secondCount;
        return new Dominance(dominant, dominantCount, contested);
    }

    private static void updateBossBar(MinecraftServer minecraftServer, CaptureGameData data, CapturePoint point) {
        ServerBossEvent bossBar = BOSS_BARS.computeIfAbsent(point.id(), id -> new ServerBossEvent(
                Component.literal(point.displayName()),
                BossEvent.BossBarColor.WHITE,
                BossEvent.BossBarOverlay.PROGRESS
        ));

        Scoreboard scoreboard = minecraftServer.getScoreboard();
        String owner = point.ownerTeam();
        String capturing = point.capturingTeam();
        float progress = point.progress();
        int percent = Math.round(progress * 100.0F);

        Component title;
        Team colorTeam = null;
        BossEvent.BossBarColor barColor = BossEvent.BossBarColor.WHITE;

        if (owner != null && progress >= 1.0F) {
            colorTeam = scoreboard.getPlayerTeam(owner);
            title = Component.translatable(
                    "bossbar.zovcapture.title",
                    point.displayName(),
                    percent,
                    TeamColors.teamDisplayName(colorTeam, owner)
            );
            barColor = data.resolveTeamBossColor(owner, TeamColors.teamBossColor(colorTeam));
        } else if (capturing != null) {
            colorTeam = scoreboard.getPlayerTeam(capturing);
            title = Component.translatable(
                    "bossbar.zovcapture.title",
                    point.displayName(),
                    percent,
                    TeamColors.teamDisplayName(colorTeam, capturing)
            );
            barColor = data.resolveTeamBossColor(capturing, TeamColors.teamBossColor(colorTeam));
        } else if (owner != null && progress < 1.0F) {
            title = Component.translatable(
                    "bossbar.zovcapture.neutralizing",
                    point.displayName(),
                    percent
            );
        } else {
            title = Component.translatable(
                    "bossbar.zovcapture.neutral",
                    point.displayName(),
                    percent
            );
        }

        bossBar.setName(title);
        bossBar.setProgress(Math.clamp(progress, 0.0F, 1.0F));
        bossBar.setColor(barColor);
        bossBar.setVisible(true);
    }

    private static void refreshBossBarViewers(MinecraftServer minecraftServer) {
        for (ServerBossEvent bossBar : BOSS_BARS.values()) {
            bossBar.removeAllPlayers();
        }

        for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
            for (ServerBossEvent bossBar : BOSS_BARS.values()) {
                bossBar.addPlayer(player);
            }
        }
    }

    public static void rebuildBossBars(MinecraftServer minecraftServer) {
        BOSS_BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BOSS_BARS.clear();

        CaptureGameData data = CaptureGameData.get(minecraftServer.overworld());
        for (CapturePoint point : data.points().values()) {
            updateBossBar(minecraftServer, data, point);
        }
        refreshBossBarViewers(minecraftServer);
    }

    public static void removeBossBar(String pointId) {
        ServerBossEvent bossBar = BOSS_BARS.remove(pointId);
        if (bossBar != null) {
            bossBar.removeAllPlayers();
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (server != null) {
            CaptureNetworking.syncToPlayer(player);
        }
    }

    public static String normalizeId(String raw) {
        return raw.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public static CaptureGameData getData(ServerLevel level) {
        return CaptureGameData.get(level);
    }

    private record Dominance(@Nullable String dominantTeam, int dominantCount, boolean contested) {
    }
}
