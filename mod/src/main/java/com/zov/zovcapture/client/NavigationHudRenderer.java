package com.zov.zovcapture.client;

import com.zov.zovcapture.ZovCaptureMod;
import com.zov.zovcapture.network.BaseZoneSync;
import com.zov.zovcapture.network.CapturePointSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ZovCaptureMod.MOD_ID, value = Dist.CLIENT)
public final class NavigationHudRenderer {
    private NavigationHudRenderer() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientCaptureData.gameActive() || ClientCaptureData.matchFinished()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        String teamName = ClientEconomyData.teamName();
        if (teamName.isEmpty()) {
            return;
        }

        Vec3 playerPos = minecraft.player.position();
        float playerYaw = minecraft.player.getYRot();
        ResourceLocation dimension = minecraft.level.dimension().location();

        List<NavigationHelper.Target> targets = new ArrayList<>();
        for (BaseZoneSync base : ClientCaptureData.baseZones()) {
            if (teamName.equals(base.team())) {
                targets.add(new NavigationHelper.Target(
                        Component.translatable("hud.zovcapture.nav.base").getString(),
                        base.center(),
                        base.dimension(),
                        0x55FF55
                ));
                break;
            }
        }

        for (CapturePointSync point : ClientCaptureData.points()) {
            targets.add(new NavigationHelper.Target(
                    point.displayName(),
                    point.center(),
                    point.dimension(),
                    NavigationHelper.ownerColor(point.ownerTeam(), teamName)
            ));
        }

        List<NavigationHelper.Entry> entries = NavigationHelper.sortByDistance(playerPos, playerYaw, targets, dimension);
        if (entries.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = screenWidth - 8;
        int y = 8;

        graphics.drawString(
                minecraft.font,
                Component.translatable("hud.zovcapture.nav.title"),
                x - minecraft.font.width(Component.translatable("hud.zovcapture.nav.title")),
                y,
                0xFFFFFF,
                true
        );
        y += 10;

        int shown = 0;
        for (NavigationHelper.Entry entry : entries) {
            if (shown >= 5) {
                break;
            }
            String line = NavigationHelper.directionArrow(entry.relativeBearing())
                    + " "
                    + Math.round(entry.distance())
                    + "m "
                    + entry.target().label();
            int width = minecraft.font.width(line);
            graphics.drawString(minecraft.font, line, x - width, y, entry.target().color(), true);
            y += 9;
            shown++;
        }

        if (entries.size() > 5) {
            Component more = Component.translatable("hud.zovcapture.nav.more", entries.size() - 5);
            graphics.drawString(
                    minecraft.font,
                    more,
                    x - minecraft.font.width(more),
                    y,
                    0xAAAAAA,
                    true
            );
        }
    }
}
