package com.zov.zovcapture.item;

import com.zov.zovcapture.ZovCaptureMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ZovCaptureItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ZovCaptureMod.MOD_ID);

    public static final DeferredItem<Item> SIEGE_BREAKER = ITEMS.register(
            "siege_breaker",
            () -> new SiegeBreakerItem(new Item.Properties().stacksTo(1).durability(Tiers.IRON.getUses()).rarity(Rarity.EPIC))
    );

    private ZovCaptureItems() {
    }
}
