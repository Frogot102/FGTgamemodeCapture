package com.zov.zovcapture.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record ShopOfferSync(
        String id,
        String displayName,
        int cost,
        String wallet,
        String type,
        String payload,
        int count,
        String category,
        List<String> bundleLines,
        int cooldownSeconds,
        int cooldownRemainingSeconds,
        String description
) {
    public static final StreamCodec<FriendlyByteBuf, ShopOfferSync> STREAM_CODEC = StreamCodec.of(
            ShopOfferSync::encode,
            ShopOfferSync::decode
    );

    private static void encode(FriendlyByteBuf buf, ShopOfferSync offer) {
        buf.writeUtf(offer.id);
        buf.writeUtf(offer.displayName);
        buf.writeVarInt(offer.cost);
        buf.writeUtf(offer.wallet);
        buf.writeUtf(offer.type);
        buf.writeUtf(offer.payload);
        buf.writeVarInt(offer.count);
        buf.writeUtf(offer.category);
        buf.writeVarInt(offer.bundleLines.size());
        for (String line : offer.bundleLines) {
            buf.writeUtf(line);
        }
        buf.writeVarInt(offer.cooldownSeconds);
        buf.writeVarInt(offer.cooldownRemainingSeconds);
        buf.writeUtf(offer.description != null ? offer.description : "");
    }

    private static ShopOfferSync decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String displayName = buf.readUtf();
        int cost = buf.readVarInt();
        String wallet = buf.readUtf();
        String type = buf.readUtf();
        String payload = buf.readUtf();
        int count = buf.readVarInt();
        String category = buf.readUtf();
        int bundleCount = buf.readVarInt();
        List<String> bundleLines = new ArrayList<>(bundleCount);
        for (int i = 0; i < bundleCount; i++) {
            bundleLines.add(buf.readUtf());
        }
        int cooldownSeconds = buf.readVarInt();
        int cooldownRemainingSeconds = buf.readVarInt();
        String description = buf.readUtf();
        return new ShopOfferSync(
                id, displayName, cost, wallet, type, payload, count, category, bundleLines,
                cooldownSeconds, cooldownRemainingSeconds, description
        );
    }
}
