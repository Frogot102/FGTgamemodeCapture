package com.zov.zovcapture.game;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public final class ParticleUtils {
    private ParticleUtils() {
    }

    public static ResourceLocation parseParticleId(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        if ("redstone".equals(normalized) || "redstone_dust".equals(normalized)) {
            return ResourceLocation.withDefaultNamespace("dust");
        }
        if (raw.contains(":")) {
            return ResourceLocation.parse(raw);
        }
        return ResourceLocation.fromNamespaceAndPath("minecraft", raw);
    }

    public static boolean isSupportedParticle(ResourceLocation particleId) {
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
        return type instanceof SimpleParticleType || type == ParticleTypes.DUST;
    }
}
