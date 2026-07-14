package com.zov.zovcapture.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ClientKeyHandler {
    private ClientKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (ZovCaptureKeys.OPEN_SHOP.consumeClick()) {
            if (!ClientEconomyData.shopAtBase()) {
                minecraft.player.displayClientMessage(
                        Component.translatable("message.zovcapture.shop.not_at_base"),
                        true
                );
                continue;
            }
            ShopScreen.open();
        }
        while (ZovCaptureKeys.OPEN_TEAMS.consumeClick()) {
            TeamSelectScreen.open();
        }
        while (ZovCaptureKeys.OPEN_MATCH.consumeClick()) {
            MatchOverviewScreen.open();
        }
        while (ZovCaptureKeys.OPEN_CLASS.consumeClick()) {
            ClassSelectScreen.open();
        }
    }
}
