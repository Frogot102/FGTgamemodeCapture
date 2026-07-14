package com.zov.zovcapture.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientMenuOpener {
    private ClientMenuOpener() {
    }

    public static void openShopMenu() {
        ShopScreen.open();
    }

    public static void openAirdropAdminMenu() {
        AirdropAdminScreen.open();
    }
}
