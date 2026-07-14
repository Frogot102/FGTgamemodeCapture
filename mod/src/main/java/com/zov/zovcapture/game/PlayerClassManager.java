package com.zov.zovcapture.game;

import com.zov.zovcapture.network.EconomyNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public final class PlayerClassManager {
    public static final int CLASS_CHANGE_COOLDOWN_SECONDS = 300;

    private PlayerClassManager() {
    }

    public static Optional<PlayerClass> getClass(CaptureGameData data, UUID playerId) {
        return PlayerClass.fromId(data.getPlayerClass(playerId));
    }

    public static boolean selectClass(ServerPlayer player, PlayerClass playerClass) {
        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        if (data.matchFinished()) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.class.match_finished"));
            return false;
        }

        Team team = player.getTeam();
        if (team == null) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.class.no_team"));
            return false;
        }

        PlayerClass previous = getClass(data, player.getUUID()).orElse(null);
        if (previous == playerClass) {
            player.sendSystemMessage(Component.translatable(
                    "message.zovcapture.class.already_selected",
                    playerClass.displayName()
            ));
            return false;
        }

        if (previous != null) {
            long now = player.server.overworld().getGameTime();
            long availableAt = data.getClassChangeAvailableAtTick(player.getUUID());
            if (now < availableAt) {
                int remainingSeconds = (int) Math.ceil((availableAt - now) / 20.0D);
                player.sendSystemMessage(Component.translatable(
                        "message.zovcapture.class.cooldown",
                        remainingSeconds / 60,
                        remainingSeconds % 60
                ));
                return false;
            }
        }

        if (playerClass == PlayerClass.CAPTAIN
                && isCaptainTaken(data, player.server, team.getName(), player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.class.captain_taken"));
            return false;
        }

        data.setPlayerClass(player.getUUID(), playerClass.id());
        updateTeamCaptain(data, team.getName(), player, previous, playerClass);

        if (previous != null) {
            long cooldownEnd = player.server.overworld().getGameTime() + CLASS_CHANGE_COOLDOWN_SECONDS * 20L;
            data.setClassChangeAvailableAtTick(player.getUUID(), cooldownEnd);
        }

        if (MatchStarterKit.isEligible(data)) {
            MatchStarterKit.giveRespawnLoadout(player, playerClass.id());
        }

        EconomyNetworking.syncPlayer(player);
        player.sendSystemMessage(Component.translatable(
                "message.zovcapture.class.selected",
                playerClass.displayName()
        ));
        return true;
    }

    public static boolean clearClass(ServerPlayer player) {
        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        if (data.matchFinished()) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.class.match_finished"));
            return false;
        }

        Team team = player.getTeam();
        if (team == null) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.class.no_team"));
            return false;
        }

        PlayerClass current = getClass(data, player.getUUID()).orElse(null);
        if (current == null) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.class.none"));
            return false;
        }

        if (current == PlayerClass.CAPTAIN) {
            UUID captain = data.getTeamCaptain(team.getName());
            if (captain != null && captain.equals(player.getUUID())) {
                data.clearTeamCaptain(team.getName());
            }
        }

        data.clearPlayerClass(player.getUUID());
        data.clearClassChangeCooldown(player.getUUID());
        EconomyNetworking.syncPlayer(player);
        player.sendSystemMessage(Component.translatable("message.zovcapture.class.cleared"));
        return true;
    }

    public static void clearClassOnTeamLeave(CaptureGameData data, ServerPlayer player, @Nullable String previousTeam) {
        PlayerClass current = getClass(data, player.getUUID()).orElse(null);
        if (current == PlayerClass.CAPTAIN && previousTeam != null) {
            UUID captain = data.getTeamCaptain(previousTeam);
            if (captain != null && captain.equals(player.getUUID())) {
                data.clearTeamCaptain(previousTeam);
            }
        }
        data.clearPlayerClass(player.getUUID());
        data.clearClassChangeCooldown(player.getUUID());
    }

    public static boolean isCaptainTaken(
            CaptureGameData data,
            MinecraftServer server,
            String teamName,
            @Nullable UUID exceptPlayer
    ) {
        for (ServerPlayer member : server.getPlayerList().getPlayers()) {
            if (exceptPlayer != null && member.getUUID().equals(exceptPlayer)) {
                continue;
            }
            Team memberTeam = member.getTeam();
            if (memberTeam == null || !teamName.equals(memberTeam.getName())) {
                continue;
            }
            if (getClass(data, member.getUUID()).orElse(null) == PlayerClass.CAPTAIN) {
                return true;
            }
        }
        return false;
    }

    private static void updateTeamCaptain(
            CaptureGameData data,
            String teamName,
            ServerPlayer player,
            @Nullable PlayerClass previous,
            PlayerClass selected
    ) {
        if (selected == PlayerClass.CAPTAIN) {
            data.setTeamCaptain(teamName, player.getUUID());
            return;
        }
        if (previous == PlayerClass.CAPTAIN) {
            UUID captain = data.getTeamCaptain(teamName);
            if (captain != null && captain.equals(player.getUUID())) {
                data.clearTeamCaptain(teamName);
            }
        }
    }
}
