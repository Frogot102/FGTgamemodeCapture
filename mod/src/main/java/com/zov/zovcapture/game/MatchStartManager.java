package com.zov.zovcapture.game;

import com.zov.zovcapture.network.CaptureNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class MatchStartManager {
    public static final int COUNTDOWN_SECONDS = 30;

    private MatchStartManager() {
    }

    public static boolean beginPreparation(MinecraftServer server, CaptureGameData data) {
        long gameTime = server.overworld().getGameTime();
        data.setCountdownEndTick(gameTime + COUNTDOWN_SECONDS * 20L);
        data.setGameActive(false);
        data.setDirty();

        int teleported = BaseSpawnManager.teleportAllTeamsToBases(server, data);
        MatchStarterKit.grantMatchStartAll(server, data);
        MatchCountdownBossBar.show(server, COUNTDOWN_SECONDS, COUNTDOWN_SECONDS);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.zovcapture.match.preparing", COUNTDOWN_SECONDS, teleported),
                false
        );
        CaptureNetworking.syncToAll(server);
        return true;
    }

    public static void tickCountdown(MinecraftServer server, CaptureGameData data) {
        long gameTime = server.overworld().getGameTime();
        if (!data.isCountdownPending()) {
            return;
        }

        if (gameTime >= data.countdownEndTick()) {
            completePreparation(server, data);
            return;
        }

        int secondsRemaining = data.countdownSecondsRemaining(gameTime);
        MatchCountdownBossBar.update(COUNTDOWN_SECONDS, secondsRemaining);
        MatchCountdownBossBar.refreshPlayers(server);
    }

    public static boolean beginInstant(MinecraftServer server, CaptureGameData data) {
        MatchCountdownBossBar.clear();
        data.setCountdownEndTick(0L);
        data.setGameActive(false);
        data.setDirty();

        int teleported = BaseSpawnManager.teleportAllTeamsToBases(server, data);
        MatchStarterKit.grantMatchStartAll(server, data);
        completePreparation(server, data);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.zovcapture.match.started.instant", teleported),
                false
        );
        return true;
    }

    public static void completePreparation(MinecraftServer server, CaptureGameData data) {
        data.setCountdownEndTick(0L);
        data.setGameActive(true);
        data.setDirty();
        MatchCountdownBossBar.clear();
        MatchRulesManager.applyMatchRules(server);
        MatchLifecycle.onMatchStart(server);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.zovcapture.match.started"),
                false
        );
        CaptureNetworking.syncToAll(server);
    }

    public static void cancelPreparation(MinecraftServer server, CaptureGameData data) {
        if (data.countdownEndTick() == 0L) {
            return;
        }
        data.setCountdownEndTick(0L);
        data.setDirty();
        MatchCountdownBossBar.clear();
        CaptureNetworking.syncToAll(server);
    }
}
