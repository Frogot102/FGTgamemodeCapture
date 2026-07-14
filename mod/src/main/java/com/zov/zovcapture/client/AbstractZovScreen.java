package com.zov.zovcapture.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public abstract class AbstractZovScreen extends Screen {
    protected static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/slot.png");

    protected static final int SLOT_SIZE = 18;
    protected static final int PANEL_MARGIN = 8;

    protected final int panelWidth;
    protected final int panelHeight;

    protected AbstractZovScreen(Component title, int panelWidth, int panelHeight) {
        super(title);
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    protected int leftPos() {
        return (this.width - panelWidth) / 2;
    }

    protected int topPos() {
        return (this.height - panelHeight) / 2;
    }

    protected void drawPanelFrame(GuiGraphics graphics) {
        int x = leftPos();
        int y = topPos();
        graphics.fill(x - 2, y - 2, x + panelWidth + 2, y + panelHeight + 2, 0x55000000);
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xF0121212);
        graphics.fill(x + 1, y + 1, x + panelWidth - 1, y + panelHeight - 1, 0xFF3A3A3A);
        graphics.fill(x + 2, y + 2, x + panelWidth - 2, y + panelHeight - 2, 0xFF1E1E1E);
        graphics.fill(x + 2, y + 2, x + panelWidth - 2, y + 16, 0xFF2F5F9A);
        graphics.fill(x + 2, y + 16, x + panelWidth - 2, y + 17, 0xFF6FA8FF);
    }

    protected void drawHeader(GuiGraphics graphics) {
        int x = leftPos() + PANEL_MARGIN;
        int y = topPos() + 4;
        graphics.drawString(this.font, this.title, x, y, 0xFFFFFF, true);
        if (shouldDrawCredit()) {
            int creditWidth = this.font.width(creditText());
            graphics.drawString(this.font, creditText(), leftPos() + panelWidth - PANEL_MARGIN - creditWidth, y, 0xAAAAAA, false);
        }
        drawSeparator(graphics, topPos() + 18);
    }

    protected boolean shouldDrawCredit() {
        return false;
    }

    protected Component creditText() {
        return Component.translatable("gui.zovcapture.shop.credit");
    }

    protected void drawSeparator(GuiGraphics graphics, int y) {
        int x = leftPos() + PANEL_MARGIN;
        graphics.fill(x, y, x + panelWidth - PANEL_MARGIN * 2, y + 1, 0xFF666666);
    }

    protected void drawInfoBar(GuiGraphics graphics, int y, int height, Component text, int color) {
        int x = leftPos() + PANEL_MARGIN;
        int width = panelWidth - PANEL_MARGIN * 2;
        graphics.fill(x, y, x + width, y + height, 0xAA1A1A1A);
        graphics.fill(x, y, x + 2, y + height, 0xFF5FA8FF);
        graphics.fill(x, y, x + width, y + 1, 0xFF505050);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF202020);
        graphics.drawString(this.font, text, x + 6, y + (height - 8) / 2, color, false);
    }

    protected void drawSectionTitle(GuiGraphics graphics, Component title, int y) {
        graphics.drawString(this.font, title, leftPos() + PANEL_MARGIN, y, 0xFFDDDDDD, false);
    }

    protected void drawMutedCentered(GuiGraphics graphics, Component text, int y) {
        graphics.drawCenteredString(this.font, text, leftPos() + panelWidth / 2, y, 0xA0A0A0);
    }

    protected void drawSlot(GuiGraphics graphics, int slotX, int slotY) {
        drawSlot(graphics, slotX, slotY, false);
    }

    protected void drawSlot(GuiGraphics graphics, int slotX, int slotY, boolean hovered) {
        graphics.blit(SLOT_TEXTURE, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE);
        if (hovered) {
            graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x44FFFFFF);
        }
    }

    protected void drawPriceBadge(GuiGraphics graphics, int slotX, int slotY, String text) {
        int width = this.font.width(text) + 4;
        int x = slotX + SLOT_SIZE - width;
        int y = slotY + SLOT_SIZE - 8;
        graphics.fill(x, y, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xCC000000);
        graphics.drawString(this.font, text, x + 2, y + 1, 0xFFFFD966, false);
    }

    protected void drawCooldownOverlay(GuiGraphics graphics, int slotX, int slotY, int seconds) {
        graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xAA000000);
        String label = seconds + "s";
        graphics.drawCenteredString(this.font, label, slotX + SLOT_SIZE / 2, slotY + 5, 0xFFFF6666);
    }

    protected void drawTabHighlight(GuiGraphics graphics, int x, int y, int width, int height, boolean active) {
        if (active) {
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF5FA8FF);
            graphics.fill(x, y, x + width, y + height, 0xFF3D7FD6);
        }
    }

    protected void renderTooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
        List<FormattedCharSequence> formatted = lines.stream()
                .map(Component::getVisualOrderText)
                .toList();
        graphics.renderTooltip(this.font, formatted, mouseX, mouseY);
    }

    protected int teamColor(String teamName) {
        if (this.minecraft.level == null) {
            return 0xFFFFFF;
        }
        var team = this.minecraft.level.getScoreboard().getPlayerTeam(teamName);
        if (team != null && team.getColor() != null && team.getColor().getColor() != null) {
            return team.getColor().getColor() | 0xFF000000;
        }
        return 0xFFFFFF;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        drawPanelFrame(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
