package com.zov.zovcapture.shop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ShopBundleHelper {
    private ShopBundleHelper() {
    }

    public static List<ItemStack> captureInventory(ServerPlayer player) {
        List<ItemStack> captured = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                captured.add(stack.copy());
            }
        }
        return captured;
    }
}
