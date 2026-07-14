package com.zov.zovcapture.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class BaseBuffManager {
    private BaseBuffManager() {
    }

    public static void tick(ServerPlayer player, CaptureGameData data) {
        if (!data.gameActive() || data.matchFinished()) {
            return;
        }

        BaseZone base = BaseSpawnManager.findTeamBase(player, data);
        if (base == null || !BaseProtectionManager.isOnPlayersTeam(player, base)) {
            return;
        }
        if (!player.level().dimension().equals(base.dimension())) {
            return;
        }
        if (!base.contains(player.blockPosition())) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
    }
}
