package com.zov.zovcapture.airdrop;

import com.zov.zovcapture.game.CaptureGameData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class AirdropLootRoller {
    private AirdropLootRoller() {
    }

    public static void rollAndGive(ServerPlayer player, CaptureGameData data) {
        List<AirdropLootEntry> entries = new ArrayList<>(data.airdropLoot().values());
        if (entries.isEmpty()) {
            return;
        }

        int totalWeight = entries.stream().mapToInt(AirdropLootEntry::weight).sum();
        if (totalWeight <= 0) {
            return;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (AirdropLootEntry entry : entries) {
            roll -= entry.weight();
            if (roll < 0) {
                applyEntry(player, data, entry);
                return;
            }
        }
        applyEntry(player, data, entries.getLast());
    }

    private static void applyEntry(ServerPlayer player, CaptureGameData data, AirdropLootEntry entry) {
        switch (entry.type()) {
            case ITEM -> giveItem(player, entry);
            case BUNDLE -> giveBundle(player, entry);
            case PERSONAL_MONEY -> data.addPersonalMoney(player.getUUID(), entry.count());
            case TEAM_MONEY -> {
                if (player.getTeam() != null) {
                    data.addTeamMoney(player.getTeam().getName(), entry.count());
                }
            }
        }
    }

    private static void giveItem(ServerPlayer player, AirdropLootEntry entry) {
        ResourceLocation itemId = ResourceLocation.parse(entry.payload());
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemId)).orElse(Items.AIR);
        if (item == Items.AIR) {
            return;
        }
        giveStack(player, new ItemStack(item, Math.max(1, entry.count())));
    }

    private static void giveBundle(ServerPlayer player, AirdropLootEntry entry) {
        for (ItemStack stack : entry.bundleItems()) {
            giveStack(player, stack.copy());
        }
    }

    private static void giveStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
