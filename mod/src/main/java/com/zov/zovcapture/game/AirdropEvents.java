package com.zov.zovcapture.game;

import com.zov.zovcapture.airdrop.AirdropManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class AirdropEvents {
    private AirdropEvents() {
    }

    public static void onChestInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        if (AirdropManager.isAirdropChest(event.getPos())) {
            event.setCanceled(true);
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (AirdropManager.isAirdropChest(event.getPos())) {
            event.setCanceled(true);
            if (event.getPlayer() != null) {
                event.getPlayer().displayClientMessage(Component.translatable("message.zovcapture.airdrop.protected"), true);
            }
        }
    }
}
