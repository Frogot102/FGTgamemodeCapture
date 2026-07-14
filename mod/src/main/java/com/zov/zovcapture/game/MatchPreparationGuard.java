package com.zov.zovcapture.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;

public final class MatchPreparationGuard {
    private MatchPreparationGuard() {
    }

    public static void enforce(MinecraftServer server, CaptureGameData data) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = player.getTeam();
            if (team == null) {
                continue;
            }

            BaseZone base = data.findBaseForTeam(team.getName());
            if (base == null) {
                continue;
            }

            if (!player.level().dimension().equals(base.dimension()) || !base.contains(player.blockPosition())) {
                if (BaseSpawnManager.teleportToTeamBase(player, data)) {
                    player.displayClientMessage(Component.translatable("message.zovcapture.match.base_locked"), true);
                }
            }
        }
    }
}
