package com.zov.zovcapture.tools;

import com.zov.zovcapture.presets.MatchBalancePreset;
import com.zov.zovcapture.shop.ShopOffer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Patches saved match presets with balanced economy, airdrop loot and shop offers
 * without changing capture points or their settings.
 */
public final class PresetPatcher {
    private PresetPatcher() {
    }

    public static void patchTest1(Path gameDirectory) throws IOException {
        patch(gameDirectory.resolve("zovcapture/presets/test1.dat"), gameDirectory.resolve("zovcapture/shops/sbw_balance.dat"));
    }

    public static void patch(Path presetFile, Path shopFile) throws IOException {
        if (!Files.exists(presetFile)) {
            throw new IOException("Preset not found: " + presetFile);
        }

        CompoundTag preset = NbtIo.readCompressed(presetFile, NbtAccounter.unlimitedHeap());
        preset.put("EconomyRules", MatchBalancePreset.economyTag());
        preset.put("AirdropLoot", MatchBalancePreset.airdropLootTag());
        preset.put("ShopOffers", buildShopOffers(shopFile));

        NbtIo.writeCompressed(preset, presetFile);
    }

    private static ListTag buildShopOffers(Path shopFile) throws IOException {
        ListTag offers = new ListTag();
        if (Files.exists(shopFile)) {
            CompoundTag shopRoot = NbtIo.readCompressed(shopFile, NbtAccounter.unlimitedHeap());
            if (shopRoot.contains("Offers")) {
                offers = filterShopOffers(shopRoot.getList("Offers", Tag.TAG_COMPOUND));
            }
        }

        ListTag withoutMaterials = new ListTag();
        for (Tag entry : offers) {
            CompoundTag tag = (CompoundTag) entry;
            if (MatchBalancePreset.isBasicMaterialOffer(tag.getString("Id"))) {
                continue;
            }
            withoutMaterials.add(entry.copy());
        }
        offers = withoutMaterials;

        for (ShopOffer offer : MatchBalancePreset.basicMaterialOffers()) {
            offers.add(offerTag(offer));
        }
        return offers;
    }

    private static ListTag filterShopOffers(ListTag saved) {
        ListTag filtered = new ListTag();
        for (Tag entry : saved) {
            CompoundTag tag = (CompoundTag) entry;
            String type = tag.getString("Type");
            String category = tag.getString("Category");
            String payload = tag.getString("Payload");
            String id = tag.getString("Id");
            if ("COMMAND".equals(type)) {
                continue;
            }
            if ("VEHICLES".equals(category)) {
                continue;
            }
            if (isLooseAmmo(payload, id)) {
                continue;
            }
            if (isWeaponPart(id, payload)) {
                continue;
            }
            filtered.add(entry.copy());
        }
        return filtered;
    }

    private static boolean isLooseAmmo(String payload, String id) {
        String lowerPayload = payload.toLowerCase();
        String lowerId = id.toLowerCase();
        if (lowerPayload.endsWith("_ammo_box") || lowerId.contains("ammo_box")) {
            return false;
        }
        return lowerPayload.endsWith("_ammo") || lowerId.contains("taser_electrode");
    }

    private static boolean isWeaponPart(String id, String payload) {
        String shortId = payload.contains(":") ? payload.substring(payload.indexOf(':') + 1) : id;
        return switch (shortId) {
            case "mortar_base_plate", "mortar_barrel", "mortar_bipod" -> true;
            default -> id.contains("mortar_base_plate") || id.contains("mortar_barrel") || id.contains("mortar_bipod");
        };
    }

    private static CompoundTag offerTag(ShopOffer offer) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", offer.id());
        tag.putString("DisplayName", offer.displayName());
        tag.putInt("Cost", offer.cost());
        tag.putString("Wallet", offer.wallet().name());
        tag.putString("Type", offer.type().name());
        tag.putString("Payload", offer.payload());
        tag.putInt("Count", offer.count());
        tag.putString("Category", offer.category().name());
        tag.putInt("CooldownSeconds", offer.cooldownSeconds());
        tag.putString("ItemClass", offer.itemClass().name());
        return tag;
    }

    public static void main(String[] args) throws IOException {
        Path gameDir = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        patchTest1(gameDir);
        System.out.println("Patched preset: " + gameDir.resolve("zovcapture/presets/test1.dat"));
    }
}
