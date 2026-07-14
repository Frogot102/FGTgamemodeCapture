package com.zov.zovcapture.shop;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum ShopCategory {
    BLOCKS,
    TOOLS,
    WEAPONS,
    ARMOR,
    VEHICLES;

    private static final Map<String, ShopCategory> ALIASES = Map.ofEntries(
            Map.entry("blocks", BLOCKS),
            Map.entry("block", BLOCKS),
            Map.entry("bloki", BLOCKS),
            Map.entry("blok", BLOCKS),
            Map.entry("блоки", BLOCKS),
            Map.entry("блок", BLOCKS),
            Map.entry("tools", TOOLS),
            Map.entry("tool", TOOLS),
            Map.entry("instrumenty", TOOLS),
            Map.entry("instrument", TOOLS),
            Map.entry("инструменты", TOOLS),
            Map.entry("инструмент", TOOLS),
            Map.entry("weapons", WEAPONS),
            Map.entry("weapon", WEAPONS),
            Map.entry("oruzhie", WEAPONS),
            Map.entry("oruzhije", WEAPONS),
            Map.entry("оружие", WEAPONS),
            Map.entry("armor", ARMOR),
            Map.entry("armour", ARMOR),
            Map.entry("bronya", ARMOR),
            Map.entry("brona", ARMOR),
            Map.entry("броня", ARMOR),
            Map.entry("vehicles", VEHICLES),
            Map.entry("vehicle", VEHICLES),
            Map.entry("tech", VEHICLES),
            Map.entry("tehnika", VEHICLES),
            Map.entry("tekhnika", VEHICLES),
            Map.entry("техника", VEHICLES)
    );

    public static Optional<ShopCategory> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        ShopCategory alias = ALIASES.get(normalized);
        if (alias != null) {
            return Optional.of(alias);
        }
        try {
            return Optional.of(valueOf(normalized.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public String translationKey() {
        return "gui.zovcapture.shop.category." + name().toLowerCase(Locale.ROOT);
    }
}
