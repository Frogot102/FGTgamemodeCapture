package com.zov.zovcapture.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShopOffer {
    public enum WalletType {
        PERSONAL,
        TEAM
    }

    public enum OfferType {
        ITEM,
        BUNDLE,
        TEAM_STAT,
        COMMAND
    }

    private final String id;
    private String displayName;
    private int cost;
    private WalletType wallet;
    private OfferType type;
    private String payload;
    private int count;
    private ShopCategory category;
    private int cooldownSeconds;
    private ShopItemClass itemClass;
    private final String description;
    private final List<ItemStack> bundleItems;

    public ShopOffer(
            String id,
            String displayName,
            int cost,
            WalletType wallet,
            OfferType type,
            String payload,
            int count,
            ShopCategory category,
            int cooldownSeconds,
            List<ItemStack> bundleItems
    ) {
        this(id, displayName, cost, wallet, type, payload, count, category, cooldownSeconds, ShopItemClass.ALL, bundleItems, "");
    }

    public ShopOffer(
            String id,
            String displayName,
            int cost,
            WalletType wallet,
            OfferType type,
            String payload,
            int count,
            ShopCategory category,
            int cooldownSeconds,
            ShopItemClass itemClass,
            List<ItemStack> bundleItems
    ) {
        this(id, displayName, cost, wallet, type, payload, count, category, cooldownSeconds, itemClass, bundleItems, "");
    }

    public ShopOffer(
            String id,
            String displayName,
            int cost,
            WalletType wallet,
            OfferType type,
            String payload,
            int count,
            ShopCategory category,
            int cooldownSeconds,
            ShopItemClass itemClass,
            List<ItemStack> bundleItems,
            String description
    ) {
        this.id = id;
        this.displayName = displayName;
        this.cost = cost;
        this.wallet = wallet;
        this.type = type;
        this.payload = payload;
        this.count = count;
        this.category = category;
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.itemClass = itemClass != null ? itemClass : ShopItemClass.ALL;
        this.description = description != null ? description : "";
        this.bundleItems = new ArrayList<>(bundleItems);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int cost() {
        return cost;
    }

    public WalletType wallet() {
        return wallet;
    }

    public OfferType type() {
        return type;
    }

    public String payload() {
        return payload;
    }

    public int count() {
        return count;
    }

    public ShopCategory category() {
        return category;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public ShopItemClass itemClass() {
        return itemClass;
    }

    public List<ItemStack> bundleItems() {
        return Collections.unmodifiableList(bundleItems);
    }

    public String description() {
        return description;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("DisplayName", displayName);
        tag.putInt("Cost", cost);
        tag.putString("Wallet", wallet.name());
        tag.putString("Type", type.name());
        tag.putString("Payload", payload);
        tag.putInt("Count", count);
        tag.putString("Category", category.name());
        tag.putInt("CooldownSeconds", cooldownSeconds);
        tag.putString("ItemClass", itemClass.name());
        if (!description.isBlank()) {
            tag.putString("Description", description);
        }

        if (type == OfferType.BUNDLE && !bundleItems.isEmpty()) {
            ListTag items = new ListTag();
            for (ItemStack stack : bundleItems) {
                items.add(stack.save(provider));
            }
            tag.put("BundleItems", items);
        }
        return tag;
    }

    public static ShopOffer load(CompoundTag tag, HolderLookup.Provider provider) {
        OfferType type = OfferType.valueOf(tag.getString("Type"));
        ShopCategory category = tag.contains("Category")
                ? ShopCategory.valueOf(tag.getString("Category"))
                : defaultCategory(type);

        List<ItemStack> bundleItems = new ArrayList<>();
        if (tag.contains("BundleItems")) {
            ListTag items = tag.getList("BundleItems", Tag.TAG_COMPOUND);
            for (Tag entry : items) {
                bundleItems.add(ItemStack.parseOptional(provider, (CompoundTag) entry));
            }
        }

        return new ShopOffer(
                tag.getString("Id"),
                tag.getString("DisplayName"),
                tag.getInt("Cost"),
                WalletType.valueOf(tag.getString("Wallet")),
                type,
                tag.getString("Payload"),
                tag.getInt("Count"),
                category,
                tag.contains("CooldownSeconds") ? tag.getInt("CooldownSeconds") : 0,
                tag.contains("ItemClass") ? ShopItemClass.fromString(tag.getString("ItemClass")) : ShopItemClass.ALL,
                bundleItems,
                tag.contains("Description") ? tag.getString("Description") : ""
        );
    }

    private static ShopCategory defaultCategory(OfferType type) {
        return switch (type) {
            case ITEM, BUNDLE -> ShopCategory.BLOCKS;
            case TEAM_STAT, COMMAND -> ShopCategory.TOOLS;
        };
    }

    public List<String> bundlePreviewLines() {
        List<String> lines = new ArrayList<>();
        for (ItemStack stack : bundleItems) {
            if (stack.isEmpty()) {
                continue;
            }
            lines.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getCount());
        }
        return lines;
    }
}
