package com.zov.zovcapture.game;

import net.minecraft.server.level.ServerPlayer;

public final class BaseShopAccess {
    private BaseShopAccess() {
    }

    public static boolean isOnTeamBase(ServerPlayer player, CaptureGameData data) {
        BaseZone base = BaseSpawnManager.findTeamBase(player, data);
        if (base == null) {
            return false;
        }
        if (!player.level().dimension().equals(base.dimension())) {
            return false;
        }
        return base.contains(player.blockPosition());
    }
}
