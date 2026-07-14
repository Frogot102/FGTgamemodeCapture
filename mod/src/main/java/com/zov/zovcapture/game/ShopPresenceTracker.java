package com.zov.zovcapture.game;

import com.zov.zovcapture.network.EconomyNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShopPresenceTracker {
    private static final Map<UUID, Boolean> LAST_AT_BASE = new HashMap<>();

    private ShopPresenceTracker() {
    }

    public static void tick(ServerPlayer player, CaptureGameData data) {
        if (player.tickCount % 10 != 0) {
            return;
        }
        boolean atBase = BaseShopAccess.isOnTeamBase(player, data);
        Boolean previous = LAST_AT_BASE.put(player.getUUID(), atBase);
        if (previous == null || previous != atBase) {
            EconomyNetworking.syncPlayer(player);
        }
    }

    public static void clear(UUID playerId) {
        LAST_AT_BASE.remove(playerId);
    }
}
