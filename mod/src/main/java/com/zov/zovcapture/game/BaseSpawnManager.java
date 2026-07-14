package com.zov.zovcapture.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

public final class BaseSpawnManager {
    private BaseSpawnManager() {
    }

    public static boolean teleportToTeamBase(ServerPlayer player, CaptureGameData data) {
        BaseZone base = findTeamBase(player, data);
        if (base == null) {
            return false;
        }

        ServerLevel level = player.server.getLevel(base.dimension());
        if (level == null) {
            return false;
        }

        Optional<Vec3> spawn = findSafeSpawn(level, base);
        if (spawn.isEmpty()) {
            return false;
        }

        Vec3 position = spawn.get();
        if (player.level().dimension().equals(base.dimension())) {
            player.teleportTo(position.x, position.y, position.z);
        } else {
            player.teleportTo(level, position.x, position.y, position.z, Set.of(), player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        return true;
    }

    public static int teleportAllTeamsToBases(MinecraftServer server, CaptureGameData data) {
        int teleported = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (teleportToTeamBase(player, data)) {
                teleported++;
            }
        }
        return teleported;
    }

    @Nullable
    public static BaseZone findTeamBase(ServerPlayer player, CaptureGameData data) {
        Team team = player.getTeam();
        if (team == null) {
            return null;
        }
        return data.findBaseForTeam(team.getName());
    }

    public static Optional<Vec3> findSafeSpawn(ServerLevel level, BaseZone zone) {
        BlockPos center = zone.center();
        Optional<Vec3> atCenter = tryAround(level, zone, center.getX(), center.getZ(), center.getY());
        if (atCenter.isPresent()) {
            return atCenter;
        }

        int radius = zone.radius();
        for (int ring = 1; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    BlockPos sample = new BlockPos(x, center.getY(), z);
                    if (!zone.contains(sample)) {
                        continue;
                    }
                    Optional<Vec3> found = tryAround(level, zone, x, z, center.getY());
                    if (found.isPresent()) {
                        return found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Vec3> tryAround(ServerLevel level, BaseZone zone, int x, int z, int hintY) {
        for (int dy = 8; dy >= -8; dy--) {
            BlockPos feet = new BlockPos(x, hintY + dy, z);
            if (!zone.contains(feet)) {
                continue;
            }
            if (isSafeStandingPosition(level, feet)) {
                return Optional.of(new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D));
            }
        }

        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        BlockPos feet = surface.above();
        if (zone.contains(feet) && isSafeStandingPosition(level, feet)) {
            return Optional.of(new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D));
        }
        return Optional.empty();
    }

    private static boolean isSafeStandingPosition(ServerLevel level, BlockPos feet) {
        BlockState ground = level.getBlockState(feet.below());
        BlockState lower = level.getBlockState(feet);
        BlockState upper = level.getBlockState(feet.above());

        if (!ground.isSolidRender(level, feet.below())) {
            return false;
        }
        if (ground.is(BlockTags.FIRE) || ground.is(BlockTags.CAMPFIRES)) {
            return false;
        }
        if (!lower.isAir() || !upper.isAir()) {
            return false;
        }
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) {
            return false;
        }
        return true;
    }
}
