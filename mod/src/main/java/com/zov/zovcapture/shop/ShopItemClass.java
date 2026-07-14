package com.zov.zovcapture.shop;

import com.zov.zovcapture.game.PlayerClass;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;

public enum ShopItemClass {
    ALL,
    ASSAULT,
    MEDIC,
    ENGINEER,
    SCOUT;

    public static ShopItemClass fromCsv(@Nullable String csvClass) {
        if (csvClass == null || csvClass.isBlank() || "Все".equalsIgnoreCase(csvClass.trim())) {
            return ALL;
        }
        return PlayerClass.fromCsvLabel(csvClass)
                .map(playerClass -> switch (playerClass) {
                    case ASSAULT -> ASSAULT;
                    case MEDIC -> MEDIC;
                    case ENGINEER -> ENGINEER;
                    case SCOUT -> SCOUT;
                    case CAPTAIN -> ALL;
                })
                .orElse(ALL);
    }

    public static ShopItemClass fromString(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ALL;
        }
    }

    public static Optional<ShopItemClass> specificClass(@Nullable String raw) {
        ShopItemClass value = fromString(raw);
        return value == ALL ? Optional.empty() : Optional.of(value);
    }

    public boolean visibleTo(@Nullable PlayerClass playerClass) {
        if (playerClass == null) {
            return false;
        }
        if (playerClass.hasFullShopAccess()) {
            return true;
        }
        if (this == ALL) {
            return true;
        }
        return switch (playerClass) {
            case ASSAULT -> this == ASSAULT;
            case MEDIC -> this == MEDIC;
            case ENGINEER -> this == ENGINEER;
            case SCOUT -> this == SCOUT;
            case CAPTAIN -> true;
        };
    }
}
