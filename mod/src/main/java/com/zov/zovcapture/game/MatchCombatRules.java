package com.zov.zovcapture.game;

import com.zov.zovcapture.presets.MatchBalancePreset;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class MatchCombatRules {
    public static final float FALL_DAMAGE_MULTIPLIER = 2.0F;

    private MatchCombatRules() {
    }

    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        event.setDamageMultiplier(event.getDamageMultiplier() * FALL_DAMAGE_MULTIPLIER);
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (TeamFriendlyFireRules.isSameTeamFriendlyFire(victim, event.getSource())) {
            event.setCanceled(true);
        }
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (TeamFriendlyFireRules.isSameTeamFriendlyFire(victim, event.getSource())) {
            event.setNewDamage(0.0F);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        ShopPresenceTracker.tick(player, data);

        if (data.matchFinished()) {
            return;
        }

        MatchParticipationRules.enforceTeamRequirement(player, data);

        if (!data.gameActive()) {
            return;
        }

        var food = player.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(20.0F);
        food.setExhaustion(0.0F);

        RespawnPenaltyManager.tick(player, data);
        BaseBuffManager.tick(player, data);
    }

    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        if (!data.gameActive() || data.matchFinished()) {
            return;
        }

        if (player.hasEffect(MobEffects.REGENERATION)) {
            return;
        }

        if (event.getAmount() > 1.0F) {
            return;
        }

        event.setCanceled(true);
    }

    public static void onPlayerDeath(ServerPlayer player, CaptureGameData data) {
        if (!data.gameActive() || data.matchFinished()) {
            return;
        }

        long releaseAt = player.server.overworld().getGameTime()
                + MatchBalancePreset.RESPAWN_COOLDOWN_SECONDS * 20L;
        data.setRespawnPenaltyUntil(player.getUUID(), releaseAt);
        player.displayClientMessage(
                Component.translatable(
                        "message.zovcapture.respawn.cooldown",
                        MatchBalancePreset.RESPAWN_COOLDOWN_SECONDS
                ),
                true
        );
    }

    public static void onPlayerRespawn(ServerPlayer player, CaptureGameData data) {
        if (!data.gameActive() && !data.isCountdownPending()) {
            data.clearRespawnPenalty(player.getUUID());
            return;
        }
        if (data.matchFinished()) {
            data.clearRespawnPenalty(player.getUUID());
            return;
        }

        RespawnPenaltyManager.applyIfNeeded(player, data);
    }
}
