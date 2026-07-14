package com.zov.zovcapture.game;

import com.zov.zovcapture.network.EconomyNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MatchLifecycle {
    private MatchLifecycle() {
    }

    public static void onMatchStart(MinecraftServer server) {
        CaptureGameData data = CaptureGameData.get(server.overworld());
        if (MatchStatManager.hasAnyRules(data)) {
            MatchStatManager.applyAll(server, data);
        }
    }

    public static void onMatchEnd(MinecraftServer server, boolean revertStats) {
        MatchRulesManager.restoreMatchRules(server);
        if (revertStats) {
            MatchStatManager.revertAll(server);
        }
    }

    public static void onPlayerJoinMatch(ServerPlayer player) {
        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        long gameTime = player.server.overworld().getGameTime();
        if (data.isMatchPreparing(gameTime)) {
            BaseSpawnManager.teleportToTeamBase(player, data);
            MatchStarterKit.grantMatchStart(player, data);
            MatchCountdownBossBar.refreshPlayers(player.server);
            MatchPreparationGuard.enforce(player.server, data);
        }
        if (data.gameActive() && !data.matchFinished() && MatchStatManager.hasAnyRules(data)) {
            MatchStatManager.applyToPlayer(player, data);
        }
        com.zov.zovcapture.network.EconomyNetworking.syncPlayer(player);
    }
}
