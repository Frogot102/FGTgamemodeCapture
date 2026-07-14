package com.zov.zovcapture.airdrop;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

public final class ActiveAirdrop {
    private final String spawnPointId;
    private final String displayName;
    private final BlockPos cratePos;
    private final ResourceKey<Level> dimension;
    private final int radius;
    private float captureProgress;
    @Nullable
    private UUID capturingPlayer;

    public ActiveAirdrop(
            String spawnPointId,
            String displayName,
            BlockPos cratePos,
            ResourceKey<Level> dimension,
            int radius
    ) {
        this.spawnPointId = spawnPointId;
        this.displayName = displayName;
        this.cratePos = cratePos;
        this.dimension = dimension;
        this.radius = radius;
    }

    public String spawnPointId() {
        return spawnPointId;
    }

    public String displayName() {
        return displayName;
    }

    public BlockPos cratePos() {
        return cratePos;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public int radius() {
        return radius;
    }

    public float captureProgress() {
        return captureProgress;
    }

    public void setCaptureProgress(float captureProgress) {
        this.captureProgress = Math.clamp(captureProgress, 0.0F, 1.0F);
    }

    @Nullable
    public UUID capturingPlayer() {
        return capturingPlayer;
    }

    public void setCapturingPlayer(@Nullable UUID capturingPlayer) {
        this.capturingPlayer = capturingPlayer;
    }

    public boolean contains(BlockPos pos) {
        int dx = pos.getX() - cratePos.getX();
        int dz = pos.getZ() - cratePos.getZ();
        return dx * dx + dz * dz <= (double) radius * radius;
    }
}
