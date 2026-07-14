package com.zov.zovcapture.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class MatchParticipationRules {
    private MatchParticipationRules() {
    }

    public static void enforceTeamRequirement(ServerPlayer player, CaptureGameData data) {
        if (data.matchFinished()) {
            return;
        }
        if (!data.gameActive() && !data.isCountdownPending()) {
            return;
        }
        if (player.getTeam() != null || player.isSpectator()) {
            return;
        }
        player.setGameMode(GameType.SPECTATOR);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.zovcapture.team.spectator_no_team"
        ));
    }

    public static void restoreSurvivalIfEligible(ServerPlayer player, CaptureGameData data) {
        if (player.getTeam() == null) {
            return;
        }
        if (data.matchFinished()) {
            return;
        }
        if (player.isSpectator()) {
            player.setGameMode(GameType.SURVIVAL);
        }
    }
}
