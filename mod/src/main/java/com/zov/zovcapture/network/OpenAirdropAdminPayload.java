package com.zov.zovcapture.network;

import com.zov.zovcapture.client.ClientMenuOpener;
import com.zov.zovcapture.ZovCaptureMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenAirdropAdminPayload() implements CustomPacketPayload {
    public static final Type<OpenAirdropAdminPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "open_airdrop_admin"));

    public static final StreamCodec<FriendlyByteBuf, OpenAirdropAdminPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenAirdropAdminPayload());

    public static void handle(OpenAirdropAdminPayload payload, IPayloadContext context) {
        context.enqueueWork(ClientMenuOpener::openAirdropAdminMenu);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
