package com.zov.zovcapture.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ModItemHelper {
    private ModItemHelper() {
    }

    public static boolean giveItem(ServerPlayer player, ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemId)).orElse(Items.AIR);
        if (item == Items.AIR) {
            return false;
        }
        return giveStack(player, new ItemStack(item, Math.max(1, count)));
    }

    public static boolean giveStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        return true;
    }
}
