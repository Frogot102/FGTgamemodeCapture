package com.zov.zovcapture.airdrop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class AirdropSpawnPoint {
    private final String id;
    private String displayName;
    private BlockPos center;
    private int radius;
    private ResourceKey<Level> dimension;

    public AirdropSpawnPoint(String id, String displayName, BlockPos center, int radius, ResourceKey<Level> dimension) {
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

    public BlockPos center() {
        return center;
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

    public boolean contains(BlockPos pos) {
        int dx = pos.getX() - center.getX();
        int dz = pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("DisplayName", displayName);
        tag.putLong("Center", center.asLong());
        tag.putInt("Radius", radius);
        tag.putString("Dimension", dimension.location().toString());
        return tag;
    }

    public static AirdropSpawnPoint load(CompoundTag tag) {
        return new AirdropSpawnPoint(
                tag.getString("Id"),
                tag.getString("DisplayName"),
                BlockPos.of(tag.getLong("Center")),
                tag.getInt("Radius"),
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension")))
        );
    }
}
