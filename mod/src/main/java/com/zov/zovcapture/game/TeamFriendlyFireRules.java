package com.zov.zovcapture.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;

public final class TeamFriendlyFireRules {
    private TeamFriendlyFireRules() {
    }

    public static void disableForAllTeams(MinecraftServer server) {
        for (PlayerTeam team : server.getScoreboard().getPlayerTeams()) {
            team.setAllowFriendlyFire(false);
        }
    }

    public static void disableForTeam(@Nullable Team team) {
        if (team instanceof PlayerTeam playerTeam) {
            playerTeam.setAllowFriendlyFire(false);
        }
    }

    public static boolean isSameTeamFriendlyFire(ServerPlayer victim, DamageSource source) {
        ServerPlayer attacker = resolveAttackingPlayer(source);
        if (attacker == null || attacker.getUUID().equals(victim.getUUID())) {
            return false;
        }

        Team victimTeam = victim.getTeam();
        Team attackerTeam = attacker.getTeam();
        if (victimTeam == null || attackerTeam == null) {
            return false;
        }

        return victimTeam.getName().equals(attackerTeam.getName());
    }

    @Nullable
    private static ServerPlayer resolveAttackingPlayer(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getEntity() instanceof Player player && player instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        if (source.getDirectEntity() instanceof Player player && player instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        return null;
    }
}
