package com.zov.zovcapture.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public final class BaseZone {
    private final String id;
    private String displayName;
    private BlockPos center;
    private int radius;
    private ResourceKey<Level> dimension;
    @Nullable
    private String team;

    public BaseZone(String id, String displayName, BlockPos center, int radius, ResourceKey<Level> dimension, @Nullable String team) {
        this.id = id;
        this.displayName = displayName;
        this.center = center;
        this.radius = radius;
        this.dimension = dimension;
        this.team = team;
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

    @Nullable
    public String team() {
        return team;
    }

    public void setTeam(@Nullable String team) {
        this.team = team;
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
        if (team != null) {
            tag.putString("Team", team);
        }
        return tag;
    }

    public static BaseZone load(CompoundTag tag) {
        String id = tag.getString("Id");
        String displayName = tag.contains("DisplayName") ? tag.getString("DisplayName") : id;
        BlockPos center = BlockPos.of(tag.getLong("Center"));
        int radius = tag.getInt("Radius");
        ResourceKey<Level> dimension = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("Dimension"))
        );
        String team = tag.contains("Team") ? tag.getString("Team") : null;
        if (team != null && team.isEmpty()) {
            team = null;
        }
        return new BaseZone(id, displayName, center, radius, dimension, team);
    }
}
