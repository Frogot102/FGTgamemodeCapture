package com.zov.zovcapture.client;

import com.zov.zovcapture.ZovCaptureConfig;
import com.zov.zovcapture.network.BaseZoneSync;
import com.zov.zovcapture.network.CapturePointSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class ZoneParticleRenderer {
    private static int tickCounter = 0;

    private ZoneParticleRenderer() {
    }

    public static void tick() {
        if (!ZovCaptureConfig.SHOW_PARTICLES.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.level instanceof ClientLevel level) || minecraft.player == null) {
            return;
        }

        if (++tickCounter % 4 != 0) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        Vec3 playerPos = minecraft.player.position();
        int density = ZovCaptureConfig.PARTICLE_DENSITY.get();

        renderCapturePoints(level, dimension, playerPos, density);
        renderBaseZones(level, dimension, playerPos, density);
        renderAirdropZone(level, dimension, playerPos, density);
    }

    private static void renderCapturePoints(
            ClientLevel level,
            ResourceKey<Level> dimension,
            Vec3 playerPos,
            int density
    ) {
        for (CapturePointSync point : ClientCaptureData.points()) {
            if (!point.dimension().equals(dimension.location())) {
                continue;
            }

            double centerX = point.center().getX() + 0.5;
            double centerZ = point.center().getZ() + 0.5;
            double dx = playerPos.x - centerX;
            double dz = playerPos.z - centerZ;
            if (dx * dx + dz * dz > (point.radius() + 128) * (point.radius() + 128)) {
                continue;
            }

            double groundY = findGroundY(level, point.center()) + 0.15;
            ParticleContext context = resolveCaptureContext(point);
            ParticleType<?> particle = ParticleSpawnHelper.resolveParticle(context.teamName(), context.neutral());
            if (particle == null) {
                continue;
            }

            spawnEdgeRing(level, particle, centerX, groundY, centerZ, point.radius(), density);
        }
    }

    private static void renderBaseZones(
            ClientLevel level,
            ResourceKey<Level> dimension,
            Vec3 playerPos,
            int density
    ) {
        if (!ClientCaptureData.baseZoneParticlesEnabled()) {
            return;
        }

        ParticleType<?> particle = ParticleSpawnHelper.resolveBaseZoneParticle();
        if (particle == null) {
            return;
        }

        for (BaseZoneSync zone : ClientCaptureData.baseZones()) {
            if (!zone.dimension().equals(dimension.location())) {
                continue;
            }

            double centerX = zone.center().getX() + 0.5;
            double centerZ = zone.center().getZ() + 0.5;
            double dx = playerPos.x - centerX;
            double dz = playerPos.z - centerZ;
            if (dx * dx + dz * dz > (zone.radius() + 128) * (zone.radius() + 128)) {
                continue;
            }

            double groundY = findGroundY(level, zone.center()) + 0.35;
            spawnEdgeRing(level, particle, centerX, groundY, centerZ, zone.radius(), density);
        }
    }

    private static void renderAirdropZone(
            ClientLevel level,
            ResourceKey<Level> dimension,
            Vec3 playerPos,
            int density
    ) {
        var state = ClientCaptureData.airdropState();
        if (!state.enabled() || !state.active() || !state.dimension().equals(dimension.location())) {
            return;
        }

        ParticleType<?> particle = ParticleSpawnHelper.resolveAirdropParticle();
        if (particle == null) {
            return;
        }

        double centerX = state.cratePos().getX() + 0.5;
        double centerZ = state.cratePos().getZ() + 0.5;
        double dx = playerPos.x - centerX;
        double dz = playerPos.z - centerZ;
        if (dx * dx + dz * dz > (state.radius() + 128) * (state.radius() + 128)) {
            return;
        }

        double groundY = findGroundY(level, state.cratePos()) + 0.15;
        int ringDensity = Math.max(4, Math.min(8, density / 3));
        spawnLowEdgeRing(level, particle, centerX, groundY, centerZ, state.radius(), ringDensity);
        ParticleSpawnHelper.spawnChestPillar(level, particle, centerX, state.cratePos().getY() + 1.0, centerZ);
    }

    private static void spawnLowEdgeRing(
            ClientLevel level,
            ParticleType<?> particle,
            double centerX,
            double y,
            double centerZ,
            int radius,
            int density
    ) {
        for (int i = 0; i < density; i++) {
            double angle = (Math.PI * 2.0 * i) / density;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            ParticleSpawnHelper.spawnLowEdgeParticle(level, particle, x, y, z);
        }
    }

    private static ParticleContext resolveCaptureContext(CapturePointSync point) {
        if (point.ownerTeam() != null && point.progress() >= 1.0F) {
            return new ParticleContext(point.ownerTeam(), false);
        }
        if (point.capturingTeam() != null) {
            return new ParticleContext(point.capturingTeam(), false);
        }
        if (point.ownerTeam() != null && point.progress() < 1.0F) {
            return new ParticleContext(null, true);
        }
        return new ParticleContext(null, true);
    }

    private static double findGroundY(Level level, net.minecraft.core.BlockPos center) {
        for (int offset = 0; offset <= 8; offset++) {
            if (!level.getBlockState(center.above(offset)).isAir()) {
                return center.getY() + offset;
            }
            if (offset > 0 && !level.getBlockState(center.below(offset)).isAir()) {
                return center.getY() - offset + 1;
            }
        }
        return center.getY();
    }

    private static void spawnEdgeRing(
            ClientLevel level,
            ParticleType<?> particle,
            double centerX,
            double y,
            double centerZ,
            int radius,
            int density
    ) {
        for (int i = 0; i < density; i++) {
            double angle = (Math.PI * 2.0 * i) / density;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            ParticleSpawnHelper.spawnEdgeParticle(level, particle, x, y, z);
        }
    }

    private record ParticleContext(@Nullable String teamName, boolean neutral) {
    }
}
