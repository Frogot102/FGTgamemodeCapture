package com.zov.zovcapture.client;

import com.zov.zovcapture.game.TeamVisualSettings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public final class ParticleSpawnHelper {
    private ParticleSpawnHelper() {
    }

    @Nullable
    public static ParticleType<?> resolveParticle(@Nullable String teamName, boolean neutral) {
        if (neutral) {
            return resolveType(ClientCaptureData.neutralParticle());
        }

        if (teamName != null) {
            var visual = ClientCaptureData.teamVisual(teamName);
            if (visual != null && !visual.particle().isEmpty()) {
                ParticleType<?> type = resolveType(ResourceLocation.parse(visual.particle()));
                if (type != null) {
                    return type;
                }
            }
        }

        return resolveType(TeamVisualSettings.DEFAULT_PARTICLE);
    }

    @Nullable
    public static ParticleType<?> resolveBaseZoneParticle() {
        return resolveType(ClientCaptureData.baseZoneParticle());
    }

    @Nullable
    public static ParticleType<?> resolveAirdropParticle() {
        return resolveType(ClientCaptureData.airdropState().particle());
    }

    @Nullable
    private static ParticleType<?> resolveType(ResourceLocation id) {
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(id);
        if (type == null || type == ParticleTypes.TOTEM_OF_UNDYING) {
            return null;
        }
        return type;
    }

    public static void spawnEdgeParticle(ClientLevel level, ParticleType<?> type, double x, double y, double z) {
        ParticleOptions options = createOptions(type);
        if (options == null) {
            return;
        }

        for (int layer = 0; layer < 4; layer++) {
            double offsetY = y + layer * 0.65;
            level.addParticle(options, true, x + randomOffset(), offsetY, z + randomOffset(), 0.0, 0.04, 0.0);
            level.addParticle(options, true, x + randomOffset(), offsetY + 0.15, z + randomOffset(), 0.0, 0.02, 0.0);
        }
    }

    public static void spawnLowEdgeParticle(ClientLevel level, ParticleType<?> type, double x, double y, double z) {
        ParticleOptions options = createOptions(type);
        if (options == null) {
            return;
        }
        level.addParticle(options, true, x + randomOffset(), y, z + randomOffset(), 0.0, 0.02, 0.0);
        level.addParticle(options, true, x + randomOffset(), y + 0.35, z + randomOffset(), 0.0, 0.01, 0.0);
    }

    public static void spawnChestPillar(ClientLevel level, ParticleType<?> type, double x, double y, double z) {
        ParticleOptions options = createOptions(type);
        if (options == null) {
            return;
        }

        for (int height = 0; height < 24; height += 2) {
            double offsetY = y + height;
            level.addParticle(options, true, x + randomOffset() * 0.15, offsetY, z + randomOffset() * 0.15, 0.0, 0.06, 0.0);
            if (height % 4 == 0) {
                level.addParticle(options, true, x, offsetY + 1.0, z, 0.0, 0.04, 0.0);
            }
        }
    }

    @Nullable
    private static ParticleOptions createOptions(ParticleType<?> type) {
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        if (type == ParticleTypes.DUST) {
            return new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.0F);
        }
        return null;
    }

    private static double randomOffset() {
        return (Math.random() - 0.5) * 0.22;
    }
}
