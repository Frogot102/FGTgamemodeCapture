package com.zov.zovcapture.client;

import com.zov.zovcapture.network.BaseZoneSync;
import com.zov.zovcapture.network.CapturePointSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class NavigationScreen extends AbstractZovScreen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 220;

    public NavigationScreen() {
        super(Component.translatable("gui.zovcapture.navigation.title"), PANEL_WIDTH, PANEL_HEIGHT);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new NavigationScreen());
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                Component.translatable("gui.zovcapture.menu.close"),
                button -> onClose()
        ).bounds(leftPos() + panelWidth - 78, topPos() + panelHeight - 24, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanelFrame(graphics);
        drawHeader(graphics);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        String teamName = ClientEconomyData.teamName();
        int x = leftPos() + PANEL_MARGIN;
        int y = topPos() + 24;

        if (teamName.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.zovcapture.navigation.no_team"), x, y, 0xFFAAAA, false);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        Vec3 playerPos = minecraft.player.position();
        float playerYaw = minecraft.player.getYRot();
        ResourceLocation dimension = minecraft.level.dimension().location();

        List<NavigationHelper.Target> targets = new ArrayList<>();
        for (BaseZoneSync base : ClientCaptureData.baseZones()) {
            if (teamName.equals(base.team())) {
                targets.add(new NavigationHelper.Target(
                        Component.translatable("gui.zovcapture.navigation.base", base.displayName()).getString(),
                        base.center(),
                        base.dimension(),
                        0x55FF55
                ));
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
            graphics.drawString(this.font, Component.translatable("gui.zovcapture.navigation.empty"), x, y, 0xFFAAAA, false);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        graphics.drawString(this.font, Component.translatable("gui.zovcapture.navigation.hint"), x, y, 0xAAAAAA, false);
        y += 12;

        int maxLines = 10;
        for (int i = 0; i < Math.min(entries.size(), maxLines); i++) {
            NavigationHelper.Entry entry = entries.get(i);
            String line = NavigationHelper.directionArrow(entry.relativeBearing())
                    + " "
                    + Math.round(entry.distance())
                    + "m | "
                    + NavigationHelper.compassDirection(entry.worldBearing())
                    + " | "
                    + entry.target().label();
            graphics.drawString(this.font, line, x, y, entry.target().color(), false);
            y += 10;
        }

        if (entries.size() > maxLines) {
            graphics.drawString(
                    this.font,
                    Component.translatable("gui.zovcapture.navigation.more", entries.size() - maxLines),
                    x,
                    y,
                    0x888888,
                    false
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
