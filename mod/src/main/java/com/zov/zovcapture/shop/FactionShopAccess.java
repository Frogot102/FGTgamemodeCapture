package com.zov.zovcapture.shop;

import com.zov.zovcapture.game.TeamFaction;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;

public final class FactionShopAccess {
    private FactionShopAccess() {
    }

    public static boolean isOfferVisible(@Nullable Team team, ShopOffer offer) {
        String id = offer.id();
        if (id.endsWith("_vsrf")) {
            return team != null && TeamFaction.fromTeam(team) == TeamFaction.VSRF;
        }
        if (id.endsWith("_nato")) {
            return team != null && TeamFaction.fromTeam(team) == TeamFaction.NATO;
        }
        return true;
    }
}
