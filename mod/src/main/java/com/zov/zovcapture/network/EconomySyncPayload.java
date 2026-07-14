package com.zov.zovcapture.network;



import com.zov.zovcapture.ZovCaptureMod;

import net.minecraft.network.RegistryFriendlyByteBuf;

import net.minecraft.network.codec.StreamCodec;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;



import java.util.ArrayList;

import java.util.List;



public record EconomySyncPayload(

        int personalMoney,

        int teamMoney,

        boolean captain,

        String teamName,

        String playerClassId,

        List<ShopOfferSync> shopOffers,

        boolean shopAtBase,

        int teamMoneyPulse

) implements CustomPacketPayload {

    public static final Type<EconomySyncPayload> TYPE =

            new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ZovCaptureMod.MOD_ID, "economy_sync"));



    public static final StreamCodec<RegistryFriendlyByteBuf, EconomySyncPayload> STREAM_CODEC = StreamCodec.of(

            EconomySyncPayload::encode,

            EconomySyncPayload::decode

    );



    private static void encode(RegistryFriendlyByteBuf buf, EconomySyncPayload payload) {

        buf.writeVarInt(payload.personalMoney());

        buf.writeVarInt(payload.teamMoney());

        buf.writeBoolean(payload.captain());

        buf.writeUtf(payload.teamName());

        buf.writeUtf(payload.playerClassId());

        buf.writeVarInt(payload.shopOffers().size());

        for (ShopOfferSync offer : payload.shopOffers()) {

            ShopOfferSync.STREAM_CODEC.encode(buf, offer);

        }

        buf.writeBoolean(payload.shopAtBase());

        buf.writeVarInt(payload.teamMoneyPulse());

    }



    private static EconomySyncPayload decode(RegistryFriendlyByteBuf buf) {

        int personal = buf.readVarInt();

        int team = buf.readVarInt();

        boolean captain = buf.readBoolean();

        String teamName = buf.readUtf();

        String playerClassId = buf.readUtf();

        int size = buf.readVarInt();

        List<ShopOfferSync> offers = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {

            offers.add(ShopOfferSync.STREAM_CODEC.decode(buf));

        }

        boolean shopAtBase = buf.readBoolean();

        int teamMoneyPulse = buf.readVarInt();

        return new EconomySyncPayload(personal, team, captain, teamName, playerClassId, offers, shopAtBase, teamMoneyPulse);

    }



    @Override

    public Type<? extends CustomPacketPayload> type() {

        return TYPE;

    }

}


