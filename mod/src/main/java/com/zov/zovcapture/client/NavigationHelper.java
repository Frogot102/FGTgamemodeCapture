package com.zov.zovcapture.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public final class NavigationHelper {
    private static final String[] COMPASS = {"Ю", "ЮЗ", "З", "СЗ", "С", "СВ", "В", "ЮВ"};

    private NavigationHelper() {
    }

    public record Target(String label, BlockPos center, ResourceLocation dimension, int color) {
    }

    public record Entry(Target target, double distance, float worldBearing, float relativeBearing) {
    }

    public static double horizontalDistance(Vec3 from, BlockPos to) {
        double dx = to.getX() + 0.5D - from.x;
        double dz = to.getZ() + 0.5D - from.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static float worldBearing(Vec3 from, BlockPos to) {
        double dx = to.getX() + 0.5D - from.x;
        double dz = to.getZ() + 0.5D - from.z;
        return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)));
    }

    public static float relativeBearing(float playerYaw, float bearingToTarget) {
        return Mth.wrapDegrees(bearingToTarget - playerYaw);
    }

    public static String compassDirection(float bearing) {
        int index = Math.floorMod(Math.round(Mth.wrapDegrees(bearing) / 45.0F), 8);
        return COMPASS[index];
    }

    public static String directionArrow(float relative) {
        float wrapped = Mth.wrapDegrees(relative);
        if (wrapped >= -22.5F && wrapped < 22.5F) {
            return "↑";
        }
        if (wrapped >= 22.5F && wrapped < 67.5F) {
            return "↗";
        }
        if (wrapped >= 67.5F && wrapped < 112.5F) {
            return "→";
        }
        if (wrapped >= 112.5F && wrapped < 157.5F) {
            return "↘";
        }
        if (wrapped >= 157.5F || wrapped < -157.5F) {
            return "↓";
        }
        if (wrapped >= -157.5F && wrapped < -112.5F) {
            return "↙";
        }
        if (wrapped >= -112.5F && wrapped < -67.5F) {
            return "←";
        }
        return "↖";
    }

    public static int ownerColor(@Nullable String ownerTeam, String playerTeam) {
        if (ownerTeam == null || ownerTeam.isBlank()) {
            return 0xCCCCCC;
        }
        if (ownerTeam.equals(playerTeam)) {
            return 0x55AAFF;
        }
        return 0xFF5555;
    }

    public static List<Entry> sortByDistance(Vec3 from, float playerYaw, List<Target> targets, ResourceLocation currentDimension) {
        return targets.stream()
                .filter(target -> target.dimension().equals(currentDimension))
                .map(target -> {
                    float bearing = worldBearing(from, target.center());
                    return new Entry(
                            target,
                            horizontalDistance(from, target.center()),
                            bearing,
                            relativeBearing(playerYaw, bearing)
                    );
                })
                .sorted(Comparator.comparingDouble(Entry::distance))
                .toList();
    }
}
