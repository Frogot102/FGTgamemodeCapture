package com.zov.zovcapture.shop;

import com.mojang.logging.LogUtils;
import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.presets.MatchBalancePreset;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import com.zov.zovcapture.util.VehicleContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;

public final class BalanceShopPreset {
    public static final String PRESET_ID = "sbw_balance";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> BANNED_WEAPONS = Set.of(
            "ql_1031",
            "secondary_cataclysm",
            "sentinel",
            "aurelia_sceptre",
            "bocek",
            "devotion",
            "insidious"
    );
    private static final Set<String> EXCLUDED_SHOP_ITEMS = Set.of(
            "trachelium",
            "ge_helmet_m_35",
            "armor_plate",
            "us_chest_iotv",
            "us_helmet_pasgt",
            "ru_chest_6b43",
            "ru_helmet_6b47"
    );

    private BalanceShopPreset() {
    }

    public static List<String> listBuiltInPresets() {
        return List.of(PRESET_ID);
    }

    public static void applyTestEconomy(com.zov.zovcapture.game.CaptureGameData data) {
        MatchBalancePreset.applyEconomy(data);
    }

    public static boolean isBuiltIn(String name) {
        try {
            return PRESET_ID.equals(ShopStorage.normalizeFileName(name));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static Optional<List<ShopOffer>> resolve(MinecraftServer server, String name) {
        if (!isBuiltIn(name)) {
            return Optional.empty();
        }
        return build(server);
    }

    /**
     * Applies the latest built-in shop from the mod jar, saves a snapshot to disk and marks world data dirty.
     *
     * @return number of offers applied, or -1 when the preset could not be built
     */
    public static int applyTo(MinecraftServer server, CaptureGameData data, boolean withEconomy) {
        List<ShopOffer> offers = build(server).orElse(null);
        if (offers == null || offers.isEmpty()) {
            return -1;
        }

        int count = data.replaceShopOffers(offers);
        if (withEconomy) {
            applyTestEconomy(data);
        }
        data.setDirty();

        try {
            ShopStorage.save(server, PRESET_ID, offers);
        } catch (IOException exception) {
            LOGGER.warn("Shop preset {} applied in memory but could not be saved to disk", PRESET_ID, exception);
        }
        return count;
    }

    public static int applyTo(MinecraftServer server, CaptureGameData data) {
        return applyTo(server, data, false);
    }

    public static Optional<List<ShopOffer>> build(MinecraftServer server) {
        try (InputStream stream = openCsvStream()) {
            List<ShopOffer> offers = parseCsv(server, stream);
            addSupplementalOffers(server, offers);
            addBasicMaterialOffers(offers);
            if (offers.isEmpty()) {
                LOGGER.error("Shop preset {} produced zero offers", PRESET_ID);
                return Optional.empty();
            }
            LOGGER.info("Loaded shop preset {} with {} offers", PRESET_ID, offers.size());
            return Optional.of(offers);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load shop preset {}", PRESET_ID, exception);
            return Optional.empty();
        }
    }

    private static InputStream openCsvStream() throws IOException {
        InputStream direct = BalanceShopPreset.class.getResourceAsStream("/assets/zovcapture/shops/sbw_balance.csv");
        if (direct != null) {
            return direct;
        }
        throw new IOException("Missing built-in shop preset csv in mod jar");
    }

    private static List<ShopOffer> parseCsv(MinecraftServer server, InputStream stream) throws IOException {
        List<ShopOffer> offers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                return offers;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 18) {
                    continue;
                }

                String categoryRaw = cols.get(0);
                String subcategory = cols.get(1);
                String itemId = cols.get(2);
                String nameRu = cols.get(3);
                String playerClass = cols.get(4);
                String shopType = cols.get(5);
                int pricePersonal = parseInt(cols.get(14));
                int priceTeam = parseInt(cols.get(15));
                String inShop = cols.get(16);
                String notes = cols.get(17);

                if (!"Да".equalsIgnoreCase(inShop) && !"yes".equalsIgnoreCase(inShop)) {
                    continue;
                }

                if (isLooseAmmo(categoryRaw, subcategory, itemId)) {
                    continue;
                }

                if (isExcludedFromShop(categoryRaw, subcategory, itemId, notes)) {
                    continue;
                }

                if (isBannedWeapon(itemId)) {
                    continue;
                }

                String shortId = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
                String displayName = formatDisplayName(nameRu, playerClass);
                ShopCategory category = mapCategory(categoryRaw, subcategory);
                ShopItemClass itemClass = ShopItemClass.fromCsv(playerClass);
                boolean vehicle = "Техника".equalsIgnoreCase(categoryRaw);
                int stackCount = resolveCount(shortId, notes);

                if (pricePersonal > 0 && isPersonalShop(shopType)) {
                    ShopOffer offer = createOffer(
                            server,
                            "sbw_p_" + shortId,
                            displayName,
                            pricePersonal,
                            ShopOffer.WalletType.PERSONAL,
                            itemId,
                            stackCount,
                            category,
                            vehicle,
                            itemClass
                    );
                    if (offer != null) {
                        offers.add(offer);
                    }
                }

                if (priceTeam > 0 && isTeamShop(shopType)) {
                    ShopOffer offer = createOffer(
                            server,
                            "sbw_t_" + shortId,
                            displayName,
                            priceTeam,
                            ShopOffer.WalletType.TEAM,
                            itemId,
                            stackCount,
                            category,
                            vehicle,
                            itemClass
                    );
                    if (offer != null) {
                        offers.add(offer);
                    }
                }
            }
        }
        return offers;
    }

    private static boolean isLooseAmmo(String categoryRaw, String subcategory, String itemId) {
        if (!"Патроны".equalsIgnoreCase(categoryRaw)) {
            return false;
        }
        String lowerSub = subcategory.toLowerCase(Locale.ROOT);
        if (lowerSub.contains("box")) {
            return false;
        }
        String lowerId = itemId.toLowerCase(Locale.ROOT);
        if (lowerId.endsWith("_ammo_box") || lowerId.endsWith(":ammo_box")) {
            return false;
        }
        return lowerId.endsWith("_ammo");
    }

    private static boolean isBannedWeapon(String itemId) {
        String shortId = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        return BANNED_WEAPONS.contains(shortId.toLowerCase(Locale.ROOT));
    }

    private static boolean isExcludedFromShop(
            String categoryRaw,
            String subcategory,
            String itemId,
            String notes
    ) {
        if ("Техника".equalsIgnoreCase(categoryRaw)) {
            return true;
        }
        if (isExcludedShopItem(itemId)) {
            return true;
        }
        return isWeaponPart(itemId, notes);
    }

    private static boolean isExcludedShopItem(String itemId) {
        String shortId = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        return EXCLUDED_SHOP_ITEMS.contains(shortId.toLowerCase(Locale.ROOT));
    }

    private static boolean isWeaponPart(String itemId, String notes) {
        if (notes != null && notes.toLowerCase(Locale.ROOT).contains("part")) {
            return true;
        }
        String shortId = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        return switch (shortId) {
            case "mortar_base_plate", "mortar_barrel", "mortar_bipod" -> true;
            default -> false;
        };
    }

    private static ShopOffer createOffer(
            MinecraftServer server,
            String offerId,
            String displayName,
            int cost,
            ShopOffer.WalletType wallet,
            String itemOrEntityId,
            int count,
            ShopCategory category,
            boolean vehicle,
            ShopItemClass itemClass
    ) {
        if (vehicle) {
            String entityId = itemOrEntityId.contains(":") ? itemOrEntityId : "superbwarfare:" + itemOrEntityId;
            String command = "execute as {player} at @s positioned ~3 ~ ~ run summon "
                    + entityId + " ~ ~ ~";
            return new ShopOffer(
                    offerId,
                    displayName,
                    cost,
                    wallet,
                    ShopOffer.OfferType.COMMAND,
                    command,
                    1,
                    ShopCategory.VEHICLES,
                    0,
                    itemClass,
                    List.of()
            );
        }

        ResourceLocation itemLocation = ResourceLocation.parse(itemOrEntityId);
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemLocation)).orElse(Items.AIR);
        if (item == Items.AIR) {
            LOGGER.warn("Shop preset skipped missing item {}", itemOrEntityId);
            return null;
        }

        return new ShopOffer(
                offerId,
                displayName,
                cost,
                wallet,
                ShopOffer.OfferType.ITEM,
                itemLocation.toString(),
                count,
                category,
                0,
                itemClass,
                List.of()
        );
    }

    private static String formatDisplayName(String nameRu, String playerClass) {
        String tag = switch (playerClass) {
            case "Штурмовик" -> "[Ш] ";
            case "Медик" -> "[М] ";
            case "Техник" -> "[Т] ";
            case "Разведчик" -> "[Р] ";
            default -> "";
        };
        return tag + nameRu;
    }

    private static ShopCategory mapCategory(String categoryRaw, String subcategory) {
        if ("Техника".equalsIgnoreCase(categoryRaw)) {
            return ShopCategory.VEHICLES;
        }
        if ("Экипировка".equalsIgnoreCase(categoryRaw) && "Armor".equalsIgnoreCase(subcategory)) {
            return ShopCategory.ARMOR;
        }
        if ("Разное".equalsIgnoreCase(categoryRaw) && "Defense".equalsIgnoreCase(subcategory)) {
            return ShopCategory.BLOCKS;
        }
        if ("Оружие".equalsIgnoreCase(categoryRaw) || "Патроны".equalsIgnoreCase(categoryRaw)) {
            return ShopCategory.WEAPONS;
        }
        return ShopCategory.TOOLS;
    }

    private static boolean isPersonalShop(String shopType) {
        return "Личный".equalsIgnoreCase(shopType) || "Оба".equalsIgnoreCase(shopType);
    }

    private static boolean isTeamShop(String shopType) {
        return "Командный".equalsIgnoreCase(shopType) || "Оба".equalsIgnoreCase(shopType);
    }

    private static int resolveCount(String shortId, String notes) {
        if ("barbed_wire".equals(shortId) || (notes != null && notes.toLowerCase(Locale.ROOT).contains("x8"))) {
            return 8;
        }
        return 1;
    }

    private static int parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(raw.replace(',', '.')));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (ch == ';' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        result.add(current.toString());
        return result;
    }

    private static void addSupplementalOffers(MinecraftServer server, List<ShopOffer> offers) {
        upsertPersonalDrone(server, offers);
        upsertScoutOffers(server, offers);
        addEngineerSmgOffers(server, offers);
        addMedicWeaponOffers(server, offers);
        addFlashlightOffers(server, offers);
        addSiegeBreakerOffer(server, offers);
        addFactionArmorOffers(server, offers);
        addFactionVehicleOffers(server, offers);
        addFactionVehicleAmmoOffers(server, offers);
    }

    private static void addFactionVehicleOffers(MinecraftServer server, List<ShopOffer> offers) {
        offers.removeIf(offer -> offer.id().startsWith("sbw_t_vehicle_"));

        addVehicleContainerOffer(server, offers, "sbw_t_vehicle_t90_vsrf", "Контейнер: T-90", 18000,
                "Снаряды: мелкокалиберные, управляемые ракеты",
                "ashvehicle:t_90");
        addVehicleContainerOffer(server, offers, "sbw_t_vehicle_bmp2_vsrf", "Контейнер: БМП-2", 18000,
                "Снаряды: мелкокалиберные, винтовочные, управляемые ракеты",
                "superbwarfare:bmp_2");
        addVehicleContainerOffer(server, offers, "sbw_t_vehicle_mi28_vsrf", "Контейнер: Mi-28", 18000,
                "Снаряды: мелкокалиберные, фугасные/бронебойные ракеты, управляемые",
                "superbwarfare:mi_28");

        addVehicleContainerOffer(server, offers, "sbw_t_vehicle_littlebird_nato", "Контейнер: AH-6 Little Bird", 7500,
                "Снаряды: мелкокалиберные, мелкокалиберные ракеты",
                "superbwarfare:ah_6");
        addVehicleContainerOffer(server, offers, "sbw_t_vehicle_abrams_nato", "Контейнер: M1A2 Abrams", 18000,
                "Снаряды: мелкокалиберные, Javelin",
                "ashvehicle:m1a2_abrams", "ashvehicle:m1a2", "ashvehicle:m1a1abrams");
        addVehicleContainerOffer(server, offers, "sbw_t_vehicle_bradley_nato", "Контейнер: M3A3 Bradley", 18000,
                "Снаряды: мелкокалиберные, TOW, винтовочные",
                "ashvehicle:m3a3-bradley");
    }

    private static void addFactionVehicleAmmoOffers(MinecraftServer server, List<ShopOffer> offers) {
        offers.removeIf(offer -> offer.id().startsWith("sbw_t_ammo_"));

        addTeamAmmoOffer(server, offers, "sbw_t_ammo_small_shell_vsrf", "[ВSRF] Мелкокалиберные снаряды ×8",
                1600, "superbwarfare:small_shell", 8, "_vsrf");
        addTeamAmmoOffer(server, offers, "sbw_t_ammo_small_rocket_vsrf", "[ВSRF] Мелкокалиберные ракеты ×8",
                2400, "superbwarfare:small_rocket", 8, "_vsrf");
        addTeamAmmoOffer(server, offers, "sbw_t_ammo_medium_rocket_he_vsrf", "[ВSRF] Фугасные ракеты ×4",
                2200, "superbwarfare:medium_rocket_he", 4, "_vsrf");
        addTeamAmmoOffer(server, offers, "sbw_t_ammo_medium_rocket_ap_vsrf", "[ВSRF] Бронебойные ракеты ×4",
                2200, "superbwarfare:medium_rocket_ap", 4, "_vsrf");
        addTeamAmmoOffer(server, offers, "sbw_t_ammo_atgm_vsrf", "[ВSRF] Управляемые ракеты ×4",
                2800, "superbwarfare:medium_anti_ground_missile", 4, "_vsrf");
        addTeamAmmoOffer(server, offers, "sbw_t_ammo_rifle_box_vsrf", "[ВSRF] Коробка винтовочных патронов ×2",
                900, "superbwarfare:rifle_ammo_box", 2, "_vsrf");

        addTeamAmmoOffer(server, offers, "sbw_t_ammo_small_shell_nato", "[NATO] Мелкокалиберные снаряды ×8",
                1600, "superbwarfare:small_shell", 8, "_nato");
        addTeamAmmoOffer(server, offers, "sbw_t_ammo_small_rocket_nato", "[NATO] Мелкокалиберные ракеты ×8",
                2400, "superbwarfare:small_rocket", 8, "_nato");
        addTeamAmmoOffer(server, offers, "sbw_t_ammo_tow_nato", "[NATO] Ракета TOW ×4",
                2800, "superbwarfare:tow_missile", 4, "_nato");
        addTeamAmmoOffer(server, offers, "sbw_t_ammo_javelin_nato", "[NATO] Ракета Javelin ×4",
                4800, "superbwarfare:javelin_missile", 4, "_nato");
        addTeamAmmoOffer(server, offers, "sbw_t_ammo_medium_rocket_he_nato", "[NATO] Фугасные ракеты ×4",
                2200, "superbwarfare:medium_rocket_he", 4, "_nato");
        addTeamAmmoOffer(server, offers, "sbw_t_ammo_rifle_box_nato", "[NATO] Коробка винтовочных патронов ×2",
                900, "superbwarfare:rifle_ammo_box", 2, "_nato");
    }

    private static void addVehicleContainerOffer(
            MinecraftServer server,
            List<ShopOffer> offers,
            String id,
            String displayName,
            int cost,
            String ammoHint,
            String primaryEntityId,
            String... fallbackEntityIds
    ) {
        Optional<ItemStack> stack = VehicleContainerHelper.createContainer(server, primaryEntityId, fallbackEntityIds);
        if (stack.isEmpty()) {
            LOGGER.warn("Shop preset skipped vehicle container {} ({})", id, primaryEntityId);
            return;
        }
        offers.add(new ShopOffer(
                id,
                displayName,
                cost,
                ShopOffer.WalletType.TEAM,
                ShopOffer.OfferType.BUNDLE,
                "superbwarfare:container",
                1,
                ShopCategory.VEHICLES,
                0,
                ShopItemClass.ALL,
                List.of(stack.get()),
                ammoHint
        ));
    }

    private static void addTeamAmmoOffer(
            MinecraftServer server,
            List<ShopOffer> offers,
            String id,
            String displayName,
            int cost,
            String itemId,
            int count,
            String factionSuffix
    ) {
        if (!id.endsWith(factionSuffix)) {
            return;
        }
        ShopOffer offer = createOffer(
                server,
                id,
                displayName,
                cost,
                ShopOffer.WalletType.TEAM,
                itemId,
                count,
                ShopCategory.WEAPONS,
                false,
                ShopItemClass.ALL
        );
        if (offer != null) {
            offers.add(offer);
        }
    }

    private static void upsertPersonalDrone(MinecraftServer server, List<ShopOffer> offers) {
        offers.removeIf(offer ->
                "superbwarfare:drone".equals(offer.payload())
                        && offer.wallet() == ShopOffer.WalletType.PERSONAL);
        ShopOffer drone = createOffer(
                server,
                "sbw_p_drone",
                "[Т] Дрон",
                1800,
                ShopOffer.WalletType.PERSONAL,
                "superbwarfare:drone",
                1,
                ShopCategory.TOOLS,
                false,
                ShopItemClass.ENGINEER
        );
        if (drone != null) {
            offers.add(drone);
        }
    }

    private static void upsertScoutOffers(MinecraftServer server, List<ShopOffer> offers) {
        offers.removeIf(offer ->
                "superbwarfare:drone".equals(offer.payload())
                        && offer.wallet() == ShopOffer.WalletType.PERSONAL
                        && offer.itemClass() == ShopItemClass.SCOUT);
        ShopOffer drone = createOffer(
                server,
                "sbw_p_scout_drone",
                "[Р] Дрон",
                900,
                ShopOffer.WalletType.PERSONAL,
                "superbwarfare:drone",
                1,
                ShopCategory.TOOLS,
                false,
                ShopItemClass.SCOUT
        );
        if (drone != null) {
            offers.add(drone);
        }

        addClassWeaponIfMissing(
                server,
                offers,
                "sbw_p_scout_monitor",
                "[Р] Монитор",
                250,
                "monitor",
                ShopItemClass.SCOUT
        );
        addScoutCamoOffers(server, offers);
        addScoutKitOffers(server, offers);
    }

    private static void addScoutKitOffers(MinecraftServer server, List<ShopOffer> offers) {
        offers.removeIf(offer ->
                offer.id().startsWith("sbw_p_scout_c4") || "sbw_p_scout_detonator".equals(offer.id()));
        offers.removeIf(offer -> "sbw_p_scout_jammer".equals(offer.id()));

        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_scout_spyglass",
                "[Р] Подзорная труба",
                200,
                "minecraft:spyglass",
                1,
                ShopCategory.TOOLS,
                ShopItemClass.SCOUT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_scout_claymore",
                "[Р] Клеймор",
                350,
                "superbwarfare:claymore_mine",
                1,
                ShopCategory.BLOCKS,
                ShopItemClass.SCOUT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_scout_dragon_tooth",
                "[Р] DragonTooth",
                200,
                "superbwarfare:blu_43_mine",
                1,
                ShopCategory.BLOCKS,
                ShopItemClass.SCOUT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_scout_at_mine",
                "[Р] Противотанковая мина ×2",
                500,
                "superbwarfare:tm_62",
                2,
                ShopCategory.BLOCKS,
                ShopItemClass.SCOUT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_scout_smoke",
                "[Р] Дымовая граната ×2",
                250,
                "superbwarfare:m18_smoke_grenade",
                2,
                ShopCategory.TOOLS,
                ShopItemClass.SCOUT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_scout_jammer",
                "[Р] Jammer",
                1500,
                "sbwdroneconfig:jammer",
                1,
                ShopCategory.TOOLS,
                ShopItemClass.SCOUT
        );
    }

    private static void addScoutCamoOffers(MinecraftServer server, List<ShopOffer> offers) {
        offers.removeIf(offer -> offer.id().startsWith("sbw_p_scout_camo"));

        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_scout_camo_helmet",
                "[Р] Маскхалат: шлем",
                500,
                "wararmbise:maskhalat_helmet",
                ShopCategory.ARMOR,
                ShopItemClass.SCOUT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_scout_camo_chest",
                "[Р] Маскхалат: нагрудник",
                750,
                "wararmbise:maskhalat_chestplate",
                ShopCategory.ARMOR,
                ShopItemClass.SCOUT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_scout_camo_legs",
                "[Р] Маскхалат: штаны",
                650,
                "wararmbise:maskhalat_leggings",
                ShopCategory.ARMOR,
                ShopItemClass.SCOUT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_scout_camo_boots",
                "[Р] Маскхалат: ботинки",
                500,
                "wararmbise:maskhalat_boots",
                ShopCategory.ARMOR,
                ShopItemClass.SCOUT
        );
    }

    private static void addFactionArmorOffers(MinecraftServer server, List<ShopOffer> offers) {
        offers.removeIf(offer -> isExcludedShopItem(offer.payload()));

        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_ru_helmet_6b47_vsrf",
                "[Ш][ВSRF] Российский шлем 6Б47",
                400,
                "superbwarfare:ru_helmet_6b47",
                ShopCategory.ARMOR,
                ShopItemClass.ASSAULT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_ru_chest_6b43_vsrf",
                "[Ш][ВSRF] Российский бронежилет 6Б43",
                750,
                "superbwarfare:ru_chest_6b43",
                ShopCategory.ARMOR,
                ShopItemClass.ASSAULT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_us_helmet_pasgt_nato",
                "[Ш][NATO] Американский шлем PASGT",
                400,
                "superbwarfare:us_helmet_pasgt",
                ShopCategory.ARMOR,
                ShopItemClass.ASSAULT
        );
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_us_chest_iotv_nato",
                "[Ш][NATO] Американский бронежилет IOTV",
                750,
                "superbwarfare:us_chest_iotv",
                ShopCategory.ARMOR,
                ShopItemClass.ASSAULT
        );
    }

    private static void addFlashlightOffers(MinecraftServer server, List<ShopOffer> offers) {
        offers.removeIf(offer -> "sbw_p_flashlight".equals(offer.id()));
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_flashlight",
                "ПНВ",
                2000,
                "nvg:night_vision_goggles_helmet",
                ShopCategory.TOOLS,
                ShopItemClass.ALL
        );
    }

    private static void addSiegeBreakerOffer(MinecraftServer server, List<ShopOffer> offers) {
        offers.removeIf(offer -> "sbw_p_siege_breaker".equals(offer.id()));
        addExternalItemIfMissing(
                server,
                offers,
                "sbw_p_siege_breaker",
                "Осадный инструмент",
                850,
                "zovcapture:siege_breaker",
                ShopCategory.TOOLS,
                ShopItemClass.ALL
        );
    }

    private static void addCommandOfferIfMissing(
            List<ShopOffer> offers,
            String id,
            String displayName,
            int cost,
            String command,
            ShopCategory category,
            ShopItemClass itemClass
    ) {
        if (offers.stream().anyMatch(offer -> offer.id().equals(id))) {
            return;
        }
        offers.add(new ShopOffer(
                id,
                displayName,
                cost,
                ShopOffer.WalletType.PERSONAL,
                ShopOffer.OfferType.COMMAND,
                command,
                1,
                category,
                0,
                itemClass,
                List.of()
        ));
    }

    private static void addExternalItemIfMissing(
            MinecraftServer server,
            List<ShopOffer> offers,
            String id,
            String displayName,
            int cost,
            String itemId,
            ShopCategory category,
            ShopItemClass itemClass
    ) {
        addExternalItemIfMissing(server, offers, id, displayName, cost, itemId, 1, category, itemClass);
    }

    private static void addExternalItemIfMissing(
            MinecraftServer server,
            List<ShopOffer> offers,
            String id,
            String displayName,
            int cost,
            String itemId,
            int count,
            ShopCategory category,
            ShopItemClass itemClass
    ) {
        if (offers.stream().anyMatch(offer -> offer.id().equals(id))) {
            return;
        }
        ShopOffer offer = createOffer(
                server,
                id,
                displayName,
                cost,
                ShopOffer.WalletType.PERSONAL,
                itemId,
                count,
                category,
                false,
                itemClass
        );
        if (offer != null) {
            offers.add(offer);
        }
    }

    private static void addEngineerSmgOffers(MinecraftServer server, List<ShopOffer> offers) {
        addClassWeaponIfMissing(server, offers, "sbw_p_engineer_mp_5", "[Т] MP5", 1150, "mp_5", ShopItemClass.ENGINEER);
        addClassWeaponIfMissing(server, offers, "sbw_p_engineer_vector", "[Т] Vector", 1700, "vector", ShopItemClass.ENGINEER);
    }

    private static void addMedicWeaponOffers(MinecraftServer server, List<ShopOffer> offers) {
        addClassWeaponIfMissing(server, offers, "sbw_p_medic_hunting_rifle", "Охотничья винтовка", 1150, "hunting_rifle", ShopItemClass.MEDIC);
        addClassWeaponIfMissing(server, offers, "sbw_p_medic_m_870", "M870 MCS", 950, "m_870", ShopItemClass.MEDIC);
        addClassWeaponIfMissing(server, offers, "sbw_p_medic_aa_12", "AA-12", 1450, "aa_12", ShopItemClass.MEDIC);
        addClassWeaponIfMissing(server, offers, "sbw_p_medic_homemade_shotgun", "Кустарный дробовик", 1050, "homemade_shotgun", ShopItemClass.MEDIC);
    }

    private static void addClassWeaponIfMissing(
            MinecraftServer server,
            List<ShopOffer> offers,
            String id,
            String displayName,
            int cost,
            String itemShortId,
            ShopItemClass itemClass
    ) {
        if (offers.stream().anyMatch(offer -> offer.id().equals(id))) {
            return;
        }
        ShopOffer offer = createOffer(
                server,
                id,
                displayName,
                cost,
                ShopOffer.WalletType.PERSONAL,
                "superbwarfare:" + itemShortId,
                1,
                ShopCategory.WEAPONS,
                false,
                itemClass
        );
        if (offer != null) {
            offers.add(offer);
        }
    }

    private static void addBasicMaterialOffers(List<ShopOffer> offers) {
        for (ShopOffer offer : MatchBalancePreset.basicMaterialOffers()) {
            boolean exists = offers.stream().anyMatch(saved -> saved.id().equals(offer.id()));
            if (!exists) {
                offers.add(offer);
            }
        }
    }
}
