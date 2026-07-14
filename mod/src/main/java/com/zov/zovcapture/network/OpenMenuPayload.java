package com.zov.zovcapture.network;

import com.zov.zovcapture.ZovCaptureMod;
import com.zov.zovcapture.client.ClientMenuOpener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenMenuPayload() implements CustomPacketPayload {
    public static final Type<OpenMenuPayload> TYPE =
            new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "open_menu"));

    public static final StreamCodec<FriendlyByteBuf, OpenMenuPayload> STREAM_CODEC = StreamCodec.unit(new OpenMenuPayload());

    public static void handle(OpenMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                ClientMenuOpener.openShopMenu();
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
