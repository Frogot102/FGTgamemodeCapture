package com.zov.zovcapture.shop;

import com.zov.zovcapture.ZovCaptureMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class ShopStorage {
    private ShopStorage() {
    }

    public static Path shopDirectory() {
        return FMLPaths.GAMEDIR.get().resolve("zovcapture").resolve("shops");
    }

    public static Path shopFile(String name) {
        return shopDirectory().resolve(normalizeFileName(name) + ".dat");
    }

    public static boolean save(MinecraftServer server, String name, Iterable<ShopOffer> offers) throws IOException {
        Files.createDirectories(shopDirectory());
        CompoundTag root = new CompoundTag();
        root.putString("Format", "zovcapture_shop_v1");
        root.putString("Name", normalizeFileName(name));

        ListTag shopList = new ListTag();
        for (ShopOffer offer : offers) {
            shopList.add(offer.save(server.registryAccess()));
        }
        root.put("Offers", shopList);

        NbtIo.writeCompressed(root, shopFile(name));
        return true;
    }

    public static List<ShopOffer> load(MinecraftServer server, String name) throws IOException {
        Path file = shopFile(name);
        if (!Files.exists(file)) {
            return List.of();
        }

        CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        ListTag shopList = root.getList("Offers", Tag.TAG_COMPOUND);
        List<ShopOffer> offers = new ArrayList<>();
        for (Tag entry : shopList) {
            offers.add(ShopOffer.load((CompoundTag) entry, server.registryAccess()));
        }
        return offers;
    }

    public static List<String> listSavedShops() throws IOException {
        Files.createDirectories(shopDirectory());
        try (Stream<Path> stream = Files.list(shopDirectory())) {
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
            throw new IllegalArgumentException("empty shop name");
        }
        if (!normalized.matches("[a-z0-9_\\-]+")) {
            throw new IllegalArgumentException("invalid shop name: " + raw);
        }
        return normalized;
    }
}
