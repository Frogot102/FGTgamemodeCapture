package com.zov.zovcapture.game;

import com.zov.zovcapture.economy.EconomyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class GameEvents {
    private GameEvents() {
    }

    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        CaptureGameData data = CaptureGameData.get(victim.server.overworld());
        MatchCombatRules.onPlayerDeath(victim, data);

        DamageSource source = event.getSource();
        Player attacker = source.getEntity() instanceof Player player ? player : null;
        if (attacker == null && source.getDirectEntity() instanceof Player direct) {
            attacker = direct;
        }
        if (attacker instanceof ServerPlayer killer) {
            EconomyManager.onPlayerKill(killer, victim);
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        if (data.matchFinished()) {
            data.clearRespawnPenalty(player.getUUID());
            return;
        }
        if (!data.gameActive() && !data.isCountdownPending()) {
            data.clearRespawnPenalty(player.getUUID());
            return;
        }

        BaseSpawnManager.teleportToTeamBase(player, data);
        MatchStarterKit.giveRespawnLoadout(player);
        MatchCombatRules.onPlayerRespawn(player, data);
    }
}
