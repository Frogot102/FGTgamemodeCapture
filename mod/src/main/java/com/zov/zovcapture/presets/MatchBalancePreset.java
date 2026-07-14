package com.zov.zovcapture.presets;

import com.zov.zovcapture.airdrop.AirdropLootEntry;
import com.zov.zovcapture.economy.EconomyRules;
import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.shop.ShopCategory;
import com.zov.zovcapture.shop.ShopItemClass;
import com.zov.zovcapture.shop.ShopOffer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MatchBalancePreset {
    public static final int KILL_REWARD_PERSONAL = 100;
    public static final int KILL_REWARD_TEAM = 150;
    public static final int HOLD_PERSONAL_PER_SECOND = 15;
    public static final int HOLD_TEAM_PER_SECOND = 25;
    public static final int HOLD_INTERVAL_SECONDS = 1;
    public static final int RESPAWN_COOLDOWN_SECONDS = 10;
    public static final int CAPTAIN_KILL_REWARD_MULTIPLIER = 2;

    public static final Set<String> BASIC_MATERIAL_OFFER_IDS = Set.of(
            "sbw_p_stone_16",
            "sbw_p_stone_32",
            "sbw_p_cobble_32",
            "sbw_p_oak_log_16",
            "sbw_p_oak_log_32",
            "sbw_p_spruce_log_16",
            "sbw_p_spruce_log_32",
            "sbw_p_oak_planks_32",
            "sbw_p_dirt_32",
            "sbw_p_sand_16",
            "sbw_p_gravel_16",
            "sbw_p_glass_8",
            "sbw_p_bricks_16",
            "sbw_p_coal_block",
            "sbw_p_charging_station"
    );

    private MatchBalancePreset() {
    }

    public static void applyEconomy(CaptureGameData data) {
        EconomyRules rules = data.economyRules();
        rules.setKillRewardPersonal(KILL_REWARD_PERSONAL);
        rules.setKillRewardTeam(KILL_REWARD_TEAM);
        rules.setHoldPointPersonalPerSecond(HOLD_PERSONAL_PER_SECOND);
        rules.setHoldPointTeamPerSecond(HOLD_TEAM_PER_SECOND);
        rules.setHoldRewardIntervalSeconds(HOLD_INTERVAL_SECONDS);
        rules.setHoldPersonalMode(EconomyRules.HoldPersonalMode.ALL_MEMBERS);
        data.setDirty();
    }

    public static CompoundTag economyTag() {
        EconomyRules rules = new EconomyRules();
        applyEconomyRules(rules);
        return rules.save();
    }

    public static void applyEconomyRules(EconomyRules rules) {
        rules.setKillRewardPersonal(KILL_REWARD_PERSONAL);
        rules.setKillRewardTeam(KILL_REWARD_TEAM);
        rules.setHoldPointPersonalPerSecond(HOLD_PERSONAL_PER_SECOND);
        rules.setHoldPointTeamPerSecond(HOLD_TEAM_PER_SECOND);
        rules.setHoldRewardIntervalSeconds(HOLD_INTERVAL_SECONDS);
        rules.setHoldPersonalMode(EconomyRules.HoldPersonalMode.ALL_MEMBERS);
    }

    public static List<AirdropLootEntry> buildAirdropLoot() {
        List<AirdropLootEntry> loot = new ArrayList<>();
        loot.add(new AirdropLootEntry("air_money_500", "500 личных", 50, AirdropLootEntry.LootType.PERSONAL_MONEY, "", 500, List.of()));
        loot.add(new AirdropLootEntry("air_money_1000", "1000 личных", 25, AirdropLootEntry.LootType.PERSONAL_MONEY, "", 1000, List.of()));
        loot.add(new AirdropLootEntry("air_money_2500", "2500 личных", 10, AirdropLootEntry.LootType.PERSONAL_MONEY, "", 2500, List.of()));
        loot.add(new AirdropLootEntry(
                "air_handgun_box",
                "Коробка пистолетных",
                8,
                AirdropLootEntry.LootType.ITEM,
                "superbwarfare:handgun_ammo_box",
                1,
                List.of()
        ));
        loot.add(new AirdropLootEntry(
                "air_rifle_box",
                "Коробка винтовочных",
                4,
                AirdropLootEntry.LootType.ITEM,
                "superbwarfare:rifle_ammo_box",
                1,
                List.of()
        ));
        loot.add(new AirdropLootEntry(
                "air_shotgun_box",
                "Коробка дробовик",
                3,
                AirdropLootEntry.LootType.ITEM,
                "superbwarfare:shotgun_ammo_box",
                1,
                List.of()
        ));
        return loot;
    }

    public static ListTag airdropLootTag() {
        ListTag list = new ListTag();
        list.add(lootTag("air_money_500", "500 личных", 50, "PERSONAL_MONEY", 500, ""));
        list.add(lootTag("air_money_1000", "1000 личных", 25, "PERSONAL_MONEY", 1000, ""));
        list.add(lootTag("air_money_2500", "2500 личных", 10, "PERSONAL_MONEY", 2500, ""));
        list.add(lootTag("air_handgun_box", "Коробка пистолетных", 8, "ITEM", 1, "superbwarfare:handgun_ammo_box"));
        list.add(lootTag("air_rifle_box", "Коробка винтовочных", 4, "ITEM", 1, "superbwarfare:rifle_ammo_box"));
        list.add(lootTag("air_shotgun_box", "Коробка дробовик", 3, "ITEM", 1, "superbwarfare:shotgun_ammo_box"));
        return list;
    }

    private static CompoundTag lootTag(
            String id,
            String displayName,
            int weight,
            String type,
            int count,
            String payload
    ) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("DisplayName", displayName);
        tag.putInt("Weight", weight);
        tag.putString("Type", type);
        tag.putInt("Count", count);
        tag.putString("Payload", payload);
        return tag;
    }

    public static List<ShopOffer> basicMaterialOffers() {
        List<ShopOffer> offers = new ArrayList<>();
        offers.add(stackOffer("sbw_p_stone_16", "Камень x16", 400, "minecraft:stone", 16));
        offers.add(stackOffer("sbw_p_stone_32", "Камень x32", 700, "minecraft:stone", 32));
        offers.add(stackOffer("sbw_p_cobble_32", "Булыжник x32", 450, "minecraft:cobblestone", 32));
        offers.add(stackOffer("sbw_p_oak_log_16", "Дубовое бревно x16", 350, "minecraft:oak_log", 16));
        offers.add(stackOffer("sbw_p_oak_log_32", "Дубовое бревно x32", 600, "minecraft:oak_log", 32));
        offers.add(stackOffer("sbw_p_spruce_log_16", "Еловое бревно x16", 350, "minecraft:spruce_log", 16));
        offers.add(stackOffer("sbw_p_spruce_log_32", "Еловое бревно x32", 600, "minecraft:spruce_log", 32));
        offers.add(stackOffer("sbw_p_oak_planks_32", "Дубовые доски x32", 480, "minecraft:oak_planks", 32));
        offers.add(stackOffer("sbw_p_dirt_32", "Земля x32", 320, "minecraft:dirt", 32));
        offers.add(stackOffer("sbw_p_sand_16", "Песок x16", 350, "minecraft:sand", 16));
        offers.add(stackOffer("sbw_p_gravel_16", "Гравий x16", 350, "minecraft:gravel", 16));
        offers.add(stackOffer("sbw_p_glass_8", "Стекло x8", 400, "minecraft:glass", 8));
        offers.add(stackOffer("sbw_p_bricks_16", "Кирпичи x16", 450, "minecraft:bricks", 16));
        offers.add(itemOffer("sbw_p_coal_block", "Угольный блок", 500, "minecraft:coal_block", 1, ShopItemClass.ALL));
        offers.add(itemOffer(
                "sbw_p_charging_station",
                "[Т] Зарядная станция",
                450,
                "superbwarfare:charging_station",
                1,
                ShopItemClass.ENGINEER
        ));
        return offers;
    }

    public static boolean isBasicMaterialOffer(String id) {
        if (BASIC_MATERIAL_OFFER_IDS.contains(id)) {
            return true;
        }
        return id.startsWith("sbw_p_stone")
                || id.startsWith("sbw_p_oak_log")
                || id.startsWith("sbw_p_spruce_log")
                || id.startsWith("sbw_p_coal_block")
                || id.startsWith("sbw_p_charging_station");
    }

    private static ShopOffer stackOffer(String id, String name, int cost, String itemId, int count) {
        return itemOffer(id, name, cost, itemId, count, ShopItemClass.ALL);
    }

    private static ShopOffer itemOffer(
            String id,
            String name,
            int cost,
            String itemId,
            int count,
            ShopItemClass itemClass
    ) {
        return new ShopOffer(
                id,
                name,
                cost,
                ShopOffer.WalletType.PERSONAL,
                ShopOffer.OfferType.ITEM,
                itemId,
                count,
                ShopCategory.BLOCKS,
                0,
                itemClass,
                List.of()
        );
    }
}
