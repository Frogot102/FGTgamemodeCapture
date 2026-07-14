package com.zov.zovcapture.network;

import com.zov.zovcapture.ZovCaptureMod;
import com.zov.zovcapture.game.PlayerClass;
import com.zov.zovcapture.game.PlayerClassManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectClassPayload(String classId) implements CustomPacketPayload {
    public static final Type<SelectClassPayload> TYPE =
            new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "select_class"));

    public static final StreamCodec<FriendlyByteBuf, SelectClassPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.classId()),
            buf -> new SelectClassPayload(buf.readUtf())
    );

    public static void handle(SelectClassPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PlayerClass playerClass = PlayerClass.fromId(payload.classId()).orElse(null);
            if (playerClass == null) {
                return;
            }
            PlayerClassManager.selectClass(player, playerClass);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
