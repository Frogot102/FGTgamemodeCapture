package com.zov.zovcapture.game;

import com.zov.zovcapture.network.CaptureNetworking;
import com.zov.zovcapture.network.EconomyNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public final class TeamJoinManager {
    private TeamJoinManager() {
    }

    public static boolean joinTeam(ServerPlayer player, String teamName) {
        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        if (data.matchFinished()) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.team.match_finished"));
            return false;
        }

        Scoreboard scoreboard = player.server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.team.not_found", teamName));
            return false;
        }

        Team previousTeam = player.getTeam();
        String previousTeamName = previousTeam != null ? previousTeam.getName() : null;
        if (previousTeamName != null && !previousTeamName.equals(teamName)) {
            PlayerClassManager.clearClassOnTeamLeave(data, player, previousTeamName);
        }

        scoreboard.removePlayerFromTeam(player.getScoreboardName());
        TeamFriendlyFireRules.disableForTeam(team);
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);

        MatchParticipationRules.restoreSurvivalIfEligible(player, data);

        player.sendSystemMessage(Component.translatable(
                "message.zovcapture.team.joined",
                team.getFormattedDisplayName()
        ));

        EconomyNetworking.syncPlayer(player);
        CaptureNetworking.syncToAll(player.server);
        return true;
    }

    public static boolean leaveTeam(ServerPlayer player) {
        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        if (data.matchFinished()) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.team.match_finished"));
            return false;
        }

        Team team = player.getTeam();
        if (team == null) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.team.no_team"));
            return false;
        }

        String teamName = team.getName();
        PlayerClassManager.clearClassOnTeamLeave(data, player, teamName);

        Scoreboard scoreboard = player.server.getScoreboard();
        scoreboard.removePlayerFromTeam(player.getScoreboardName());

        if (data.gameActive() || data.isCountdownPending()) {
            player.setGameMode(GameType.SPECTATOR);
        }

        ShopPresenceTracker.clear(player.getUUID());
        player.sendSystemMessage(Component.translatable("message.zovcapture.team.left"));
        EconomyNetworking.syncPlayer(player);
        CaptureNetworking.syncToAll(player.server);
        return true;
    }
}
