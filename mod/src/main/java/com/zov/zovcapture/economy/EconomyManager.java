package com.zov.zovcapture.economy;

import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.game.CaptureParticipation;
import com.zov.zovcapture.game.CapturePoint;
import com.zov.zovcapture.game.MatchStatsTracker;
import com.zov.zovcapture.network.EconomyNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EconomyManager {
    private static int holdRewardTickCounter;

    private EconomyManager() {
    }

    public static void onPlayerKill(ServerPlayer killer, ServerPlayer victim) {
        if (killer.getUUID().equals(victim.getUUID())) {
            return;
        }

        Team killerTeam = killer.getTeam();
        Team victimTeam = victim.getTeam();
        if (killerTeam != null && victimTeam != null && killerTeam.getName().equals(victimTeam.getName())) {
            return;
        }

        CaptureGameData data = CaptureGameData.get(killer.server.overworld());
        if (!data.gameActive() || data.matchFinished()) {
            return;
        }

        var rules = data.economyRules();
        MatchStatsTracker.recordKill(data, killer, victim);

        boolean victimIsEnemyCaptain = EconomyManager.isCaptain(data, victim);
        int rewardMultiplier = victimIsEnemyCaptain ? com.zov.zovcapture.presets.MatchBalancePreset.CAPTAIN_KILL_REWARD_MULTIPLIER : 1;

        int personalReward = rules.killRewardPersonal() * rewardMultiplier;
        if (personalReward > 0) {
            data.addPersonalMoney(killer.getUUID(), personalReward);
            MatchStatsTracker.recordMoneyEarned(
                    data,
                    killer.getUUID(),
                    killer.getGameProfile().getName(),
                    killerTeam != null ? killerTeam.getName() : "",
                    personalReward
            );
            if (victimIsEnemyCaptain) {
                killer.sendSystemMessage(Component.translatable("message.zovcapture.money.kill.personal.captain", personalReward));
            } else {
                killer.sendSystemMessage(Component.translatable("message.zovcapture.money.kill.personal", personalReward));
            }
        }

        if (killerTeam != null) {
            int teamReward = rules.killRewardTeam() * rewardMultiplier;
            if (teamReward > 0) {
                data.addTeamMoney(killerTeam.getName(), teamReward);
                notifyTeamMoneyGain(killer.server, data, killerTeam.getName(), teamReward);
            }
        }

        EconomyNetworking.syncPlayer(killer);
    }

    public static void awardHoldRewards(MinecraftServer server, CaptureGameData data) {
        var rules = data.economyRules();
        if (rules.holdPointPersonalPerSecond() <= 0 && rules.holdPointTeamPerSecond() <= 0) {
            return;
        }

        holdRewardTickCounter++;
        int intervalTicks = rules.holdRewardIntervalSeconds() * 20;
        if (holdRewardTickCounter < intervalTicks) {
            return;
        }
        holdRewardTickCounter = 0;

        Map<String, Integer> heldPoints = new HashMap<>();
        for (CapturePoint point : data.points().values()) {
            if (point.ownerTeam() != null && point.progress() >= 1.0F) {
                heldPoints.merge(point.ownerTeam(), 1, Integer::sum);
            }
        }

        if (heldPoints.isEmpty()) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = player.getTeam();
            if (team == null) {
                continue;
            }
            int pointsHeld = heldPoints.getOrDefault(team.getName(), 0);
            if (pointsHeld <= 0) {
                continue;
            }

            int personalGain = calculatePersonalHoldGain(data, rules, player, team.getName(), pointsHeld);
            if (personalGain > 0) {
                data.addPersonalMoney(player.getUUID(), personalGain);
                MatchStatsTracker.recordMoneyEarned(
                        data,
                        player.getUUID(),
                        player.getGameProfile().getName(),
                        team.getName(),
                        personalGain
                );
                EconomyNetworking.syncPlayer(player);
            }
        }

        for (Map.Entry<String, Integer> entry : heldPoints.entrySet()) {
            int teamGain = rules.holdPointTeamPerSecond() * entry.getValue() * rules.holdRewardIntervalSeconds();
            if (teamGain > 0) {
                data.addTeamMoney(entry.getKey(), teamGain);
                notifyTeamMoneyGain(server, data, entry.getKey(), teamGain);
            }
        }
    }

    private static int calculatePersonalHoldGain(
            CaptureGameData data,
            EconomyRules rules,
            ServerPlayer player,
            String teamName,
            int pointsHeldByTeam
    ) {
        if (rules.holdPointPersonalPerSecond() <= 0) {
            return 0;
        }

        int interval = rules.holdRewardIntervalSeconds();
        return switch (rules.holdPersonalMode()) {
            case ALL_MEMBERS -> rules.holdPointPersonalPerSecond() * pointsHeldByTeam * interval;
            case IN_ZONE -> {
                int pointsInZone = 0;
                for (CapturePoint point : data.points().values()) {
                    if (teamName.equals(point.ownerTeam()) && point.progress() >= 1.0F && isInPoint(player, point)) {
                        pointsInZone++;
                    }
                }
                yield rules.holdPointPersonalPerSecond() * pointsInZone * interval;
            }
            case CAPTAIN -> {
                if (!isCaptain(data, player)) {
                    yield 0;
                }
                yield rules.holdPointPersonalPerSecond() * pointsHeldByTeam * interval;
            }
        };
    }

    private static boolean isInPoint(ServerPlayer player, CapturePoint point) {
        if (!CaptureParticipation.canParticipateInCapture(player)) {
            return false;
        }
        return player.level().dimension().equals(point.dimension()) && point.contains(player.blockPosition());
    }

    public static boolean spendPersonal(CaptureGameData data, UUID playerId, int amount) {
        int balance = data.getPersonalMoney(playerId);
        if (balance < amount) {
            return false;
        }
        data.setPersonalMoney(playerId, balance - amount);
        return true;
    }

    public static boolean spendTeam(CaptureGameData data, String team, int amount) {
        int balance = data.getTeamMoney(team);
        if (balance < amount) {
            return false;
        }
        data.setTeamMoney(team, balance - amount);
        return true;
    }

    public static boolean takePersonal(CaptureGameData data, UUID playerId, int amount) {
        int balance = data.getPersonalMoney(playerId);
        data.setPersonalMoney(playerId, Math.max(0, balance - amount));
        return true;
    }

    public static boolean takeTeam(CaptureGameData data, String team, int amount) {
        int balance = data.getTeamMoney(team);
        data.setTeamMoney(team, Math.max(0, balance - amount));
        return true;
    }

    public static boolean isCaptain(CaptureGameData data, ServerPlayer player) {
        Team team = player.getTeam();
        if (team == null) {
            return false;
        }
        UUID captain = data.getTeamCaptain(team.getName());
        return captain != null && captain.equals(player.getUUID());
    }

    public static void resetHoldRewardCounter() {
        holdRewardTickCounter = 0;
    }

    private static void notifyTeamMoneyGain(MinecraftServer server, CaptureGameData data, String teamName, int amount) {
        data.pulseTeamMoneyForTeam(server, teamName, amount);
        EconomyNetworking.syncTeam(teamName, server);
    }
}
