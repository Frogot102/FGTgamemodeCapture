package com.zov.zovcapture.airdrop;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AirdropLootEntry {
    public enum LootType {
        ITEM,
        BUNDLE,
        PERSONAL_MONEY,
        TEAM_MONEY
    }

    private final String id;
    private String displayName;
    private int weight;
    private LootType type;
    private String payload;
    private int count;
    private final List<ItemStack> bundleItems;

    public AirdropLootEntry(
            String id,
            String displayName,
            int weight,
            LootType type,
            String payload,
            int count,
            List<ItemStack> bundleItems
    ) {
        this.id = id;
        this.displayName = displayName;
        this.weight = weight;
        this.type = type;
        this.payload = payload;
        this.count = count;
        this.bundleItems = new ArrayList<>(bundleItems);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int weight() {
        return weight;
    }

    public LootType type() {
        return type;
    }

    public String payload() {
        return payload;
    }

    public int count() {
        return count;
    }

    public List<ItemStack> bundleItems() {
        return Collections.unmodifiableList(bundleItems);
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("DisplayName", displayName);
        tag.putInt("Weight", weight);
        tag.putString("Type", type.name());
        tag.putString("Payload", payload);
        tag.putInt("Count", count);
        if (type == LootType.BUNDLE && !bundleItems.isEmpty()) {
            ListTag items = new ListTag();
            for (ItemStack stack : bundleItems) {
                items.add(stack.save(provider));
            }
            tag.put("BundleItems", items);
        }
        return tag;
    }

    public static AirdropLootEntry load(CompoundTag tag, HolderLookup.Provider provider) {
        LootType type = LootType.valueOf(tag.getString("Type"));
        List<ItemStack> bundleItems = new ArrayList<>();
        if (tag.contains("BundleItems")) {
            ListTag items = tag.getList("BundleItems", Tag.TAG_COMPOUND);
            for (Tag entry : items) {
                bundleItems.add(ItemStack.parseOptional(provider, (CompoundTag) entry));
            }
        }
        return new AirdropLootEntry(
                tag.getString("Id"),
                tag.getString("DisplayName"),
                tag.getInt("Weight"),
                type,
                tag.getString("Payload"),
                tag.getInt("Count"),
                bundleItems
        );
    }

    public List<String> previewLines() {
        List<String> lines = new ArrayList<>();
        if (type == LootType.BUNDLE) {
            for (ItemStack stack : bundleItems) {
                if (!stack.isEmpty()) {
                    lines.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getCount());
                }
            }
        }
        return lines;
    }
}
