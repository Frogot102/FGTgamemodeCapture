package com.zov.zovcapture.presets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class PresetStorage {
    private PresetStorage() {
    }

    public static Path presetDirectory() {
        return FMLPaths.GAMEDIR.get().resolve("zovcapture").resolve("presets");
    }

    public static Path presetFile(String name) {
        return presetDirectory().resolve(normalizeFileName(name) + ".dat");
    }

    public static void save(String name, CompoundTag preset) throws IOException {
        Files.createDirectories(presetDirectory());
        preset.putString("Format", "zovcapture_preset_v1");
        preset.putString("Name", normalizeFileName(name));
        NbtIo.writeCompressed(preset, presetFile(name));
    }

    public static CompoundTag load(String name) throws IOException {
        Path file = presetFile(name);
        if (!Files.exists(file)) {
            return new CompoundTag();
        }
        return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
    }

    public static List<String> listSavedPresets() throws IOException {
        Files.createDirectories(presetDirectory());
        try (Stream<Path> stream = Files.list(presetDirectory())) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".dat"))
                    .map(path -> path.getFileName().toString().replace(".dat", ""))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    public static String normalizeFileName(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("empty preset name");
        }
        if (!normalized.matches("[a-z0-9_\\-]+")) {
            throw new IllegalArgumentException("invalid preset name: " + raw);
        }
        return normalized;
    }
}
