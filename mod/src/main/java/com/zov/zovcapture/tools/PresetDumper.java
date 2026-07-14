package com.zov.zovcapture.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PresetDumper {
    private PresetDumper() {
    }

    public static void main(String[] args) throws Exception {
        Path preset = Path.of(args.length > 0 ? args[0] : "zovcapture/presets/test1.dat").toAbsolutePath().normalize();
        Path output = args.length > 1
                ? Path.of(args[1]).toAbsolutePath().normalize()
                : preset.getParent().resolve(preset.getFileName().toString().replace(".dat", "_dump.txt"));
        CompoundTag root = NbtIo.readCompressed(preset, NbtAccounter.unlimitedHeap());

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            dump(root, out);
        }
        System.out.println("Wrote: " + output);
    }

    private static void dump(CompoundTag root, PrintWriter out) {
        out.println("=== GENERAL ===");
        printInt(root, "PointsToWin", out);
        printBool(root, "AirdropEnabled", out);
        printInt(root, "AirdropIntervalSeconds", out);
        printInt(root, "AirdropCaptureSeconds", out);
        out.println("NeutralParticle=" + root.getString("NeutralParticle"));
        out.println("BaseZoneParticle=" + root.getString("BaseZoneParticle"));
        out.println("BaseZoneParticlesEnabled=" + root.getBoolean("BaseZoneParticlesEnabled"));

        if (root.contains("EconomyRules")) {
            CompoundTag economy = root.getCompound("EconomyRules");
            out.println("\n=== ECONOMY ===");
            printInt(economy, "KillRewardPersonal", out);
            printInt(economy, "KillRewardTeam", out);
            printInt(economy, "HoldPointPersonalPerSecond", out);
            printInt(economy, "HoldPointTeamPerSecond", out);
            printInt(economy, "HoldRewardIntervalSeconds", out);
            out.println("HoldPersonalMode=" + economy.getString("HoldPersonalMode"));
        }

        if (root.contains("Points")) {
            ListTag points = root.getList("Points", Tag.TAG_COMPOUND);
            out.println("\n=== POINTS (" + points.size() + ") ===");
            for (Tag entry : points) {
                CompoundTag point = (CompoundTag) entry;
                out.println(
                        point.getString("Id") + " | "
                                + point.getString("DisplayName") + " | radius="
                                + point.getInt("Radius") + " | center="
                                + formatPos(point.getLong("Center"))
                );
            }
        }

        if (root.contains("BaseZones")) {
            ListTag bases = root.getList("BaseZones", Tag.TAG_COMPOUND);
            out.println("\n=== BASES (" + bases.size() + ") ===");
            for (Tag entry : bases) {
                CompoundTag base = (CompoundTag) entry;
                out.println(
                        base.getString("Id") + " | team=" + base.getString("Team") + " | radius="
                                + base.getInt("Radius") + " | center="
                                + formatPos(base.getLong("Center"))
                );
            }
        }

        if (root.contains("AirdropSpawnPoints")) {
            ListTag spawns = root.getList("AirdropSpawnPoints", Tag.TAG_COMPOUND);
            out.println("\n=== AIRDROP SPAWNS (" + spawns.size() + ") ===");
            for (Tag entry : spawns) {
                CompoundTag spawn = (CompoundTag) entry;
                out.println(
                        spawn.getString("Id") + " | center="
                                + formatPos(spawn.getLong("Center")) + " | radius=" + spawn.getInt("Radius")
                );
            }
        }

        if (root.contains("AirdropLoot")) {
            ListTag loot = root.getList("AirdropLoot", Tag.TAG_COMPOUND);
            out.println("\n=== AIRDROP LOOT (" + loot.size() + ") ===");
            for (Tag entry : loot) {
                CompoundTag item = (CompoundTag) entry;
                out.println(
                        item.getString("Id") + " | " + item.getString("DisplayName") + " | weight="
                                + item.getInt("Weight") + " | type=" + item.getString("Type")
                                + " | payload=" + item.getString("Payload") + " | count=" + item.getInt("Count")
                );
            }
        }

        if (root.contains("ShopOffers")) {
            ListTag shop = root.getList("ShopOffers", Tag.TAG_COMPOUND);
            out.println("\n=== SHOP BLOCKS ===");
            for (Tag entry : shop) {
                CompoundTag offer = (CompoundTag) entry;
                String id = offer.getString("Id");
                if (!id.startsWith("sbw_p_stone") && !id.startsWith("sbw_p_oak") && !id.startsWith("sbw_p_spruce")
                        && !id.startsWith("sbw_p_coal") && !id.startsWith("sbw_p_charging")) {
                    continue;
                }
                out.println(
                        id + " | " + offer.getString("DisplayName") + " | cost=" + offer.getInt("Cost")
                                + " | class=" + offer.getString("ItemClass") + " | item=" + offer.getString("Payload")
                );
            }
            out.println("\n=== SHOP TOTAL OFFERS ===");
            out.println(shop.size());
        }
    }

    private static String formatPos(long packed) {
        BlockPos pos = BlockPos.of(packed);
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static void printInt(CompoundTag tag, String key, PrintWriter out) {
        if (tag.contains(key)) {
            out.println(key + "=" + tag.getInt(key));
        }
    }

    private static void printBool(CompoundTag tag, String key, PrintWriter out) {
        if (tag.contains(key)) {
            out.println(key + "=" + tag.getBoolean(key));
        }
    }
}
