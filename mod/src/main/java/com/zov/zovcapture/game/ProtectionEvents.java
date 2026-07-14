package com.zov.zovcapture.game;

import com.zov.zovcapture.airdrop.AirdropManager;
import com.zov.zovcapture.item.ZovCaptureItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.Iterator;
import java.util.List;

public final class ProtectionEvents {
    private ProtectionEvents() {
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getPlayer() == null) {
            return;
        }
        if (BaseProtectionManager.canBreak(event.getPlayer(), level, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.getPlayer().displayClientMessage(Component.translatable("message.zovcapture.base.protected"), true);
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        List<BlockPos> affected = event.getAffectedBlocks();
        Iterator<BlockPos> iterator = affected.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (BaseProtectionManager.isProtectedBlock(level, pos) || AirdropManager.isAirdropChest(pos)) {
                iterator.remove();
            }
        }
    }

    public static void giveSiegeBreaker(ServerPlayer player, int count) {
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ZovCaptureItems.SIEGE_BREAKER.get(), count));
    }
}
