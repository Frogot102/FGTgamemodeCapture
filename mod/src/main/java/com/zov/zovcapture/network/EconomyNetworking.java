package com.zov.zovcapture.network;

import com.zov.zovcapture.client.ClientEconomyData;
import com.zov.zovcapture.client.ShopScreen;
import com.zov.zovcapture.economy.EconomyManager;
import com.zov.zovcapture.game.BaseShopAccess;
import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.game.PlayerClass;
import com.zov.zovcapture.game.PlayerClassManager;
import com.zov.zovcapture.shop.ClassShopAccess;
import com.zov.zovcapture.shop.ShopOffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EconomyNetworking {
    private EconomyNetworking() {
    }

    public static void handleClientSync(EconomySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientEconomyData.update(
                    payload.personalMoney(),
                    payload.teamMoney(),
                    payload.captain(),
                    payload.teamName(),
                    payload.playerClassId(),
                    payload.shopOffers(),
                    payload.shopAtBase(),
                    payload.teamMoneyPulse()
            );
            ShopScreen.onEconomySync();
        });
    }

    public static void syncPlayer(ServerPlayer player) {
        player.connection.send(createPayload(player));
    }

    public static void syncTeam(String teamName, MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = player.getTeam();
            if (team != null && teamName.equals(team.getName())) {
                syncPlayer(player);
            }
        }
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayer(player);
        }
    }

    private static EconomySyncPayload createPayload(ServerPlayer player) {
        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        Team team = player.getTeam();
        String teamName = team != null ? team.getName() : "";
        int teamMoney = team != null ? data.getTeamMoney(teamName) : 0;
        long gameTime = player.server.overworld().getGameTime();
        PlayerClass playerClass = PlayerClassManager.getClass(data, player.getUUID()).orElse(null);
        String classId = playerClass != null ? playerClass.id() : "";

        List<ShopOfferSync> offers = new ArrayList<>();
        boolean shopAtBase = BaseShopAccess.isOnTeamBase(player, data);
        for (ShopOffer offer : data.shopOffers().values()) {
            if (!ClassShopAccess.isOfferVisible(data, player, team, playerClass, offer)) {
                continue;
            }
            offers.add(toSync(data, offer, player.getUUID(), gameTime));
        }

        int teamMoneyPulse = data.consumeTeamMoneyPulse(player.getUUID());

        return new EconomySyncPayload(
                data.getPersonalMoney(player.getUUID()),
                teamMoney,
                EconomyManager.isCaptain(data, player),
                teamName,
                classId,
                offers,
                shopAtBase,
                teamMoneyPulse
        );
    }

    private static ShopOfferSync toSync(CaptureGameData data, ShopOffer offer, UUID playerId, long gameTime) {
        return new ShopOfferSync(
                offer.id(),
                offer.displayName(),
                offer.cost(),
                offer.wallet().name(),
                offer.type().name(),
                offer.payload(),
                offer.count(),
                offer.category().name(),
                offer.bundlePreviewLines(),
                offer.cooldownSeconds(),
                0,
                offer.description()
        );
    }
}
