package com.zov.zovcapture.game;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MatchStatManager {
    private static final Map<UUID, PlayerSnapshot> SNAPSHOTS = new HashMap<>();

    private MatchStatManager() {
    }

    public static void applyAll(MinecraftServer server, CaptureGameData data) {
        if (!hasAnyRules(data)) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyToPlayer(player, data);
        }
    }

    public static void applyToPlayer(ServerPlayer player, CaptureGameData data) {
        MatchStatRules rules = resolveRules(player, data);
        if (rules.isEmpty()) {
            return;
        }

        captureSnapshot(player);
        applyRule(player, Attributes.MAX_HEALTH, rules.maxHealth());
        applyRule(player, Attributes.MOVEMENT_SPEED, rules.movementSpeed());
        applyRule(player, Attributes.ATTACK_DAMAGE, rules.attackDamage());
        applyRule(player, Attributes.ARMOR, rules.armor());
        applyRule(player, Attributes.ATTACK_SPEED, rules.attackSpeed());
        applyRule(player, Attributes.KNOCKBACK_RESISTANCE, rules.knockbackResistance());

        if (rules.maxHealth().isPresent()) {
            float max = (float) rules.maxHealth().getAsDouble();
            player.setHealth(Math.min(player.getHealth(), max));
        }
    }

    public static void revertAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            revertPlayer(player);
        }
        SNAPSHOTS.clear();
    }

    public static void revertPlayer(ServerPlayer player) {
        PlayerSnapshot snapshot = SNAPSHOTS.remove(player.getUUID());
        if (snapshot == null) {
            return;
        }
        snapshot.restore(player);
    }

    public static void clearRules(CaptureGameData data) {
        data.globalStatRules().clear();
        data.teamStatRules().clear();
        data.setDirty();
    }

    public static MatchStatRules resolveRules(ServerPlayer player, CaptureGameData data) {
        MatchStatRules merged = data.globalStatRules().copy();
        Team team = player.getTeam();
        if (team != null) {
            MatchStatRules teamRules = data.teamStatRules().get(team.getName());
            if (teamRules != null) {
                merged.mergeOver(teamRules);
            }
        }
        return merged;
    }

    public static boolean hasAnyRules(CaptureGameData data) {
        if (!data.globalStatRules().isEmpty()) {
            return true;
        }
        return data.teamStatRules().values().stream().anyMatch(rules -> !rules.isEmpty());
    }

    public static void setRule(
            CaptureGameData data,
            @Nullable String team,
            MatchStatRules.StatField field,
            @Nullable Double value
    ) {
        MatchStatRules rules = team == null ? data.globalStatRules() : data.getOrCreateTeamStatRules(team);
        switch (field) {
            case MAX_HEALTH -> rules.setMaxHealth(value);
            case MOVEMENT_SPEED -> rules.setMovementSpeed(value);
            case ATTACK_DAMAGE -> rules.setAttackDamage(value);
            case ARMOR -> rules.setArmor(value);
            case ATTACK_SPEED -> rules.setAttackSpeed(value);
            case KNOCKBACK_RESISTANCE -> rules.setKnockbackResistance(value);
        }
        data.setDirty();
    }

    private static void captureSnapshot(ServerPlayer player) {
        SNAPSHOTS.computeIfAbsent(player.getUUID(), ignored -> PlayerSnapshot.capture(player));
    }

    private static void applyRule(ServerPlayer player, Holder<Attribute> attribute, java.util.OptionalDouble value) {
        if (value.isEmpty()) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value.getAsDouble());
        }
    }

    private static final class PlayerSnapshot {
        private final Map<ResourceLocation, Double> baseValues = new HashMap<>();

        private PlayerSnapshot() {
        }

        static PlayerSnapshot capture(ServerPlayer player) {
            PlayerSnapshot snapshot = new PlayerSnapshot();
            store(snapshot, player, Attributes.MAX_HEALTH);
            store(snapshot, player, Attributes.MOVEMENT_SPEED);
            store(snapshot, player, Attributes.ATTACK_DAMAGE);
            store(snapshot, player, Attributes.ARMOR);
            store(snapshot, player, Attributes.ATTACK_SPEED);
            store(snapshot, player, Attributes.KNOCKBACK_RESISTANCE);
            return snapshot;
        }

        private static void store(PlayerSnapshot snapshot, ServerPlayer player, Holder<Attribute> attribute) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                player.getServer().registryAccess()
                        .registryOrThrow(Registries.ATTRIBUTE)
                        .getResourceKey(attribute.value())
                        .ifPresent(key -> snapshot.baseValues.put(key.location(), instance.getBaseValue()));
            }
        }

        void restore(ServerPlayer player) {
            var registry = player.getServer().registryAccess().registryOrThrow(Registries.ATTRIBUTE);
            for (Map.Entry<ResourceLocation, Double> entry : baseValues.entrySet()) {
                registry.getHolder(entry.getKey()).ifPresent(holder -> {
                    AttributeInstance instance = player.getAttribute(holder);
                    if (instance != null) {
                        instance.setBaseValue(entry.getValue());
                    }
                });
            }
            player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        }
    }
}
