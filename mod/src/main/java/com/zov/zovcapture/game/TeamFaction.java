package com.zov.zovcapture.game;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;
import java.util.Locale;

public enum TeamFaction {
    VSRF(
            "vsrf",
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "mp_443"),
            "message.zovcapture.faction.vsrf"
    ),
    NATO(
            "nato",
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "glock_17"),
            "message.zovcapture.faction.nato"
    );

    private final String id;
    private final ResourceLocation sidePistol;
    private final String translationKey;

    TeamFaction(String id, ResourceLocation sidePistol, String translationKey) {
        this.id = id;
        this.sidePistol = sidePistol;
        this.translationKey = translationKey;
    }

    public String id() {
        return id;
    }

    public ResourceLocation sidePistol() {
        return sidePistol;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    public static TeamFaction fromTeam(@Nullable Team team) {
        if (team == null) {
            return NATO;
        }
        String name = team.getName().toLowerCase(Locale.ROOT);
        if (name.contains("blue") || name.contains("vsrf") || name.contains("всрф")) {
            return VSRF;
        }
        if (name.contains("red") || name.contains("nato") || name.contains("нато")) {
            return NATO;
        }
        return NATO;
    }
}
