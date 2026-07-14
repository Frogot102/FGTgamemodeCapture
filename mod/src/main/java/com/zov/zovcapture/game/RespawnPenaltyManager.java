package com.zov.zovcapture.game;



import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.effect.MobEffectInstance;

import net.minecraft.world.effect.MobEffects;



public final class RespawnPenaltyManager {

    private RespawnPenaltyManager() {

    }



    public static void applyIfNeeded(ServerPlayer player, CaptureGameData data) {

        long releaseAt = data.getRespawnPenaltyUntil(player.getUUID());

        if (releaseAt <= 0L) {

            return;

        }



        long gameTime = player.server.overworld().getGameTime();

        if (gameTime >= releaseAt) {

            clearPenalty(player, data);

            return;

        }



        applyEffects(player);

    }



    public static void tick(ServerPlayer player, CaptureGameData data) {

        long releaseAt = data.getRespawnPenaltyUntil(player.getUUID());

        if (releaseAt <= 0L) {

            return;

        }



        long gameTime = player.server.overworld().getGameTime();

        if (gameTime >= releaseAt) {

            clearPenalty(player, data);

            return;

        }



        applyEffects(player);

        int secondsLeft = (int) ((releaseAt - gameTime + 19L) / 20L);

        player.displayClientMessage(Component.translatable("message.zovcapture.respawn.wait", secondsLeft), true);

    }



    private static void applyEffects(ServerPlayer player) {

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10, false, false, true));

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));

        player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);

    }



    private static void clearPenalty(ServerPlayer player, CaptureGameData data) {

        data.clearRespawnPenalty(player.getUUID());

        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        player.removeEffect(MobEffects.BLINDNESS);

    }

}


