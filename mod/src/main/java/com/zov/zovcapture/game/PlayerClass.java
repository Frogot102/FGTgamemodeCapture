package com.zov.zovcapture.game;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum PlayerClass {
    ASSAULT("assault", "Штурмовик"),
    MEDIC("medic", "Медик"),
    ENGINEER("engineer", "Техник"),
    SCOUT("scout", "Разведчик"),
    CAPTAIN("captain", "Капитан");

    private final String id;
    private final String csvLabel;

    PlayerClass(String id, String csvLabel) {
        this.id = id;
        this.csvLabel = csvLabel;
    }

    public String id() {
        return id;
    }

    public String csvLabel() {
        return csvLabel;
    }

    public Component displayName() {
        return Component.translatable("class.zovcapture." + id);
    }

    public Component description() {
        return Component.translatable("class.zovcapture." + id + ".desc");
    }

    public boolean hasFullShopAccess() {
        return this == CAPTAIN;
    }

    public static Optional<PlayerClass> fromId(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(value -> value.id.equals(normalized))
                .findFirst();
    }

    public static Optional<PlayerClass> fromCsvLabel(@Nullable String label) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        if ("Все".equalsIgnoreCase(label.trim())) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(value -> value.csvLabel.equalsIgnoreCase(label.trim()))
                .findFirst();
    }
}
