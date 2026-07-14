package com.zov.zovcapture.network;

import com.zov.zovcapture.ZovCaptureMod;
import com.zov.zovcapture.shop.ShopManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShopBuyPayload(String offerId) implements CustomPacketPayload {
    public static final Type<ShopBuyPayload> TYPE =
            new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "shop_buy"));

    public static final StreamCodec<FriendlyByteBuf, ShopBuyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.offerId()),
            buf -> new ShopBuyPayload(buf.readUtf())
    );

    public static void handle(ShopBuyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ShopManager.purchase(player, payload.offerId());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
