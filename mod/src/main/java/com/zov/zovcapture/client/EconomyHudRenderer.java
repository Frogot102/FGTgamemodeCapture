package com.zov.zovcapture.client;

import com.zov.zovcapture.ZovCaptureMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = ZovCaptureMod.MOD_ID, value = Dist.CLIENT)
public final class EconomyHudRenderer {
    private EconomyHudRenderer() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientCaptureData.gameActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 8;
        int y = minecraft.getWindow().getGuiScaledHeight() - 26;

        graphics.drawString(
                minecraft.font,
                Component.translatable("hud.zovcapture.personal_money", ClientEconomyData.personalMoney()),
                x,
                y,
                0x55FF55,
                true
        );
    }
}
