package com.zov.zovcapture.game;

import com.zov.zovcapture.item.ZovCaptureItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BaseProtectionManager {
    private BaseProtectionManager() {
    }

    public static boolean isProtectedBlock(ServerLevel level, BlockPos pos) {
        CaptureGameData data = CaptureGameData.get(level);
        ResourceKey<Level> dimension = level.dimension();
        for (BaseZone zone : data.baseZones().values()) {
            if (zone.dimension().equals(dimension) && zone.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canBreak(Player player, ServerLevel level, BlockPos pos) {
        if (!isProtectedBlock(level, pos)) {
            return true;
        }
        return hasSiegeBreaker(player);
    }

    public static boolean hasSiegeBreaker(Player player) {
        return isSiegeBreaker(player.getMainHandItem()) || isSiegeBreaker(player.getOffhandItem());
    }

    public static boolean isSiegeBreaker(ItemStack stack) {
        return stack.is(ZovCaptureItems.SIEGE_BREAKER.get());
    }

    public static boolean isOnPlayersTeam(ServerPlayer player, BaseZone zone) {
        if (zone.team() == null || player.getTeam() == null) {
            return false;
        }
        return zone.team().equals(player.getTeam().getName());
    }
}
