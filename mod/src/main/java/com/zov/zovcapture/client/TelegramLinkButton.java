package com.zov.zovcapture.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class TelegramLinkButton extends AbstractWidget {
    private static final int BLUE = 0xFF2AABEE;
    private static final int BLUE_HOVER = 0xFF3DBBF5;
    private static final int BLUE_DARK = 0xFF1E96D1;
    private static final int BORDER = 0xFF1684BE;
    private static final int BORDER_HOVER = 0xFF7FDBFF;

    private final Runnable onPress;

    private TelegramLinkButton(int x, int y, int width, Component message, Runnable onPress) {
        super(x, y, width, 22, message);
        this.onPress = onPress;
    }

    public static TelegramLinkButton create(int x, int y, int width, Component message, Runnable onPress) {
        return new TelegramLinkButton(x, y, width, message, onPress);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.active) {
            onPress.run();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        if (!this.active) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF555555);
            drawTelegramIcon(graphics, getX() + 5, getY() + 6, 0xFFAAAAAA);
            graphics.drawString(font, getMessage(), getX() + 18, getY() + 7, 0xFFCCCCCC, true);
            return;
        }

        int background = this.isHovered ? BLUE_HOVER : BLUE;
        int border = this.isHovered ? BORDER_HOVER : BORDER;

        graphics.fill(getX() + 1, getY() + 2, getX() + width + 1, getY() + height + 2, 0x55000000);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, 0x66FFFFFF);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, BLUE_DARK);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, border);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);

        drawTelegramIcon(graphics, getX() + 5, getY() + 6, 0xFFFFFFFF);
        graphics.drawString(font, getMessage(), getX() + 18, getY() + 7, 0xFFFFFFFF, true);
    }

    private static void drawTelegramIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 7, y, x + 8, y + 1, color);
        graphics.fill(x + 5, y + 1, x + 8, y + 2, color);
        graphics.fill(x + 3, y + 2, x + 8, y + 3, color);
        graphics.fill(x + 2, y + 3, x + 9, y + 4, color);
        graphics.fill(x + 3, y + 4, x + 8, y + 5, color);
        graphics.fill(x + 4, y + 5, x + 7, y + 8, color);
        graphics.fill(x + 1, y + 4, x + 3, y + 6, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
