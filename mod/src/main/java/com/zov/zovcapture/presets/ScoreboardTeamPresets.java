package com.zov.zovcapture.presets;

import com.mojang.serialization.DynamicOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.ArrayList;
import java.util.Locale;

public final class ScoreboardTeamPresets {
    private ScoreboardTeamPresets() {
    }

    public static ListTag exportTeams(Scoreboard scoreboard, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (PlayerTeam team : scoreboard.getPlayerTeams()) {
            list.add(exportTeam(team, provider));
        }
        return list;
    }

    public static int apply(Scoreboard scoreboard, CompoundTag preset, boolean merge, HolderLookup.Provider provider) {
        if (!preset.contains("ScoreboardTeams")) {
            return 0;
        }

        ListTag list = preset.getList("ScoreboardTeams", Tag.TAG_COMPOUND);
        if (!merge) {
            for (PlayerTeam existing : new ArrayList<>(scoreboard.getPlayerTeams())) {
                scoreboard.removePlayerTeam(existing);
            }
        }

        int applied = 0;
        for (Tag entry : list) {
            if (applyTeam(scoreboard, (CompoundTag) entry, provider)) {
                applied++;
            }
        }
        return applied;
    }

    private static CompoundTag exportTeam(PlayerTeam team, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", team.getName());
        putComponent(tag, "DisplayName", team.getDisplayName(), provider);
        putComponent(tag, "Prefix", team.getPlayerPrefix(), provider);
        putComponent(tag, "Suffix", team.getPlayerSuffix(), provider);
        tag.putString("Color", team.getColor().getName());
        tag.putBoolean("FriendlyFire", team.isAllowFriendlyFire());
        tag.putBoolean("SeeFriendlyInvisibles", team.canSeeFriendlyInvisibles());
        tag.putString("NameTagVisibility", team.getNameTagVisibility().name());
        tag.putString("DeathMessageVisibility", team.getDeathMessageVisibility().name());
        tag.putString("CollisionRule", team.getCollisionRule().name());
        return tag;
    }

    private static boolean applyTeam(Scoreboard scoreboard, CompoundTag tag, HolderLookup.Provider provider) {
        String name = tag.getString("Name");
        if (name.isBlank()) {
            return false;
        }

        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            team = scoreboard.addPlayerTeam(name);
        }

        team.setDisplayName(readComponent(tag, "DisplayName", provider, Component.literal(name)));
        team.setPlayerPrefix(readComponent(tag, "Prefix", provider, CommonComponents.EMPTY));
        team.setPlayerSuffix(readComponent(tag, "Suffix", provider, CommonComponents.EMPTY));
        team.setColor(parseColor(tag.getString("Color")));
        team.setAllowFriendlyFire(false);
        team.setSeeFriendlyInvisibles(tag.getBoolean("SeeFriendlyInvisibles"));
        team.setNameTagVisibility(parseVisibility(tag.getString("NameTagVisibility"), Team.Visibility.ALWAYS));
        team.setDeathMessageVisibility(parseVisibility(tag.getString("DeathMessageVisibility"), Team.Visibility.ALWAYS));
        team.setCollisionRule(parseCollision(tag.getString("CollisionRule"), Team.CollisionRule.ALWAYS));
        return true;
    }

    private static void putComponent(
            CompoundTag parent,
            String key,
            Component component,
            HolderLookup.Provider provider
    ) {
        DynamicOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        ComponentSerialization.CODEC.encodeStart(ops, component)
                .result()
                .ifPresent(encoded -> parent.put(key, encoded));
    }

    private static Component readComponent(
            CompoundTag parent,
            String key,
            HolderLookup.Provider provider,
            Component fallback
    ) {
        if (!parent.contains(key)) {
            return fallback;
        }
        DynamicOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        return ComponentSerialization.CODEC.parse(ops, parent.get(key))
                .result()
                .orElse(fallback);
    }

    private static ChatFormatting parseColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return ChatFormatting.RESET;
        }
        ChatFormatting color = ChatFormatting.getByName(raw.toLowerCase(Locale.ROOT));
        return color != null ? color : ChatFormatting.RESET;
    }

    private static Team.Visibility parseVisibility(String raw, Team.Visibility fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Team.Visibility.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static Team.CollisionRule parseCollision(String raw, Team.CollisionRule fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Team.CollisionRule.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
