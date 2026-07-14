package com.zov.zovcapture.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;

public final class TeamVisualSettings {
    public static final ResourceLocation DEFAULT_PARTICLE = ResourceLocation.withDefaultNamespace("end_rod");
    public static final ResourceLocation DEFAULT_NEUTRAL_PARTICLE = ResourceLocation.withDefaultNamespace("flame");
    public static final ResourceLocation DEFAULT_RESTORE_ZONE_PARTICLE = ResourceLocation.withDefaultNamespace("cloud");
    public static final ResourceLocation DEFAULT_BASE_ZONE_PARTICLE = DEFAULT_RESTORE_ZONE_PARTICLE;
    public static final BossEvent.BossBarColor DEFAULT_BOSS_COLOR = BossEvent.BossBarColor.WHITE;

    @Nullable
    private ResourceLocation particle;
    @Nullable
    private BossEvent.BossBarColor bossBarColor;

    public Optional<ResourceLocation> particle() {
        return Optional.ofNullable(particle);
    }

    public Optional<BossEvent.BossBarColor> bossBarColor() {
        return Optional.ofNullable(bossBarColor);
    }

    public void setParticle(@Nullable ResourceLocation particle) {
        this.particle = particle;
    }

    public void setBossBarColor(@Nullable BossEvent.BossBarColor bossBarColor) {
        this.bossBarColor = bossBarColor;
    }

    public void clear() {
        particle = null;
        bossBarColor = null;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (particle != null) {
            tag.putString("Particle", particle.toString());
        }
        if (bossBarColor != null) {
            tag.putString("BossBarColor", bossBarColor.getName());
        }
        return tag;
    }

    public static TeamVisualSettings load(CompoundTag tag) {
        TeamVisualSettings settings = new TeamVisualSettings();
        if (tag.contains("Particle")) {
            settings.particle = ResourceLocation.parse(tag.getString("Particle"));
        }
        if (tag.contains("BossBarColor")) {
            settings.bossBarColor = parseBossColor(tag.getString("BossBarColor")).orElse(null);
        }
        return settings;
    }

    @Nullable
    public static ResourceLocation parseParticleId(String raw) {
        return ParticleUtils.parseParticleId(raw);
    }

    public static Optional<BossEvent.BossBarColor> parseBossColor(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        for (BossEvent.BossBarColor color : BossEvent.BossBarColor.values()) {
            if (color.getName().equals(normalized)) {
                return Optional.of(color);
            }
        }
        return Optional.empty();
    }

    public static String bossColorSuggestions() {
        return "pink, blue, red, green, yellow, purple, white";
    }
}
