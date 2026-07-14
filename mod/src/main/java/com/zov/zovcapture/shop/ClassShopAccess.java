package com.zov.zovcapture.shop;

import com.zov.zovcapture.economy.EconomyManager;
import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.game.PlayerClass;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;

public final class ClassShopAccess {
    private ClassShopAccess() {
    }

    public static boolean isOfferVisible(
            CaptureGameData data,
            ServerPlayer player,
            @Nullable Team team,
            @Nullable PlayerClass playerClass,
            ShopOffer offer
    ) {
        if (offer.wallet() == ShopOffer.WalletType.TEAM) {
            if (team == null || !EconomyManager.isCaptain(data, player)) {
                return false;
            }
            return FactionShopAccess.isOfferVisible(team, offer);
        }
        if (playerClass == null) {
            return false;
        }
        if (playerClass.hasFullShopAccess()) {
            return true;
        }
        if (playerClass == PlayerClass.SCOUT && DronePayloadItems.isDronePayload(offer.payload())) {
            return false;
        }
        if (!FactionShopAccess.isOfferVisible(team, offer)) {
            return false;
        }
        return offer.itemClass().visibleTo(playerClass);
    }
}
