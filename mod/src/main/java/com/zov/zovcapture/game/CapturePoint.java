package com.zov.zovcapture.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public final class CapturePoint {
    private final String id;
    private String displayName;
    private BlockPos center;
    private int radius;
    private ResourceKey<Level> dimension;
    private float progress;
    @Nullable
    private String ownerTeam;
    @Nullable
    private String capturingTeam;

    public CapturePoint(String id, String displayName, BlockPos center, int radius, ResourceKey<Level> dimension) {
        this.id = id;
        this.displayName = displayName;
        this.center = center;
        this.radius = radius;
        this.dimension = dimension;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public BlockPos center() {
        return center;
    }

    public void setCenter(BlockPos center) {
        this.center = center;
    }

    public int radius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public float progress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = Math.clamp(progress, 0.0F, 1.0F);
    }

    @Nullable
    public String ownerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(@Nullable String ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    @Nullable
    public String capturingTeam() {
        return capturingTeam;
    }

    public void setCapturingTeam(@Nullable String capturingTeam) {
        this.capturingTeam = capturingTeam;
    }

    public boolean contains(BlockPos pos) {
        int dx = pos.getX() - center.getX();
        int dz = pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    public CompoundTag saveTemplate() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("DisplayName", displayName);
        tag.putLong("Center", center.asLong());
        tag.putInt("Radius", radius);
        tag.putString("Dimension", dimension.location().toString());
        return tag;
    }

    public CompoundTag save() {
        CompoundTag tag = saveTemplate();
        tag.putFloat("Progress", progress);
        if (ownerTeam != null) {
            tag.putString("OwnerTeam", ownerTeam);
        }
        if (capturingTeam != null) {
            tag.putString("CapturingTeam", capturingTeam);
        }
        return tag;
    }

    public static CapturePoint loadTemplate(CompoundTag tag) {
        String id = tag.getString("Id");
        String displayName = tag.getString("DisplayName");
        BlockPos center = BlockPos.of(tag.getLong("Center"));
        int radius = tag.getInt("Radius");
        ResourceKey<Level> dimension = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("Dimension"))
        );
        return new CapturePoint(id, displayName, center, radius, dimension);
    }

    public static CapturePoint load(CompoundTag tag) {
        CapturePoint point = loadTemplate(tag);
        if (tag.contains("Progress")) {
            point.setProgress(tag.getFloat("Progress"));
        }
        if (tag.contains("OwnerTeam")) {
            point.setOwnerTeam(tag.getString("OwnerTeam"));
        }
        if (tag.contains("CapturingTeam")) {
            point.setCapturingTeam(tag.getString("CapturingTeam"));
        }
        return point;
    }
}
