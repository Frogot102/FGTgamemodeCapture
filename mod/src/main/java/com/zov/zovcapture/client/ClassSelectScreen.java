package com.zov.zovcapture.client;

import com.zov.zovcapture.game.PlayerClass;
import com.zov.zovcapture.network.ClearClassPayload;
import com.zov.zovcapture.network.SelectClassPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClassSelectScreen extends AbstractZovScreen {
    public ClassSelectScreen() {
        super(Component.translatable("gui.zovcapture.classes.title"), 220, 270);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ClassSelectScreen());
    }

    @Override
    protected void init() {
        super.init();
        int buttonX = leftPos() + PANEL_MARGIN;
        int buttonWidth = panelWidth - PANEL_MARGIN * 2;
        int buttonY = topPos() + 52;

        for (PlayerClass playerClass : PlayerClass.values()) {
            Component label = buildClassLabel(playerClass);
            addRenderableWidget(Button.builder(label, button -> {
                PacketDistributor.sendToServer(new SelectClassPayload(playerClass.id()));
                onClose();
            }).bounds(buttonX, buttonY, buttonWidth, 22).build());
            buttonY += 24;
        }

        if (!ClientEconomyData.playerClassId().isEmpty() && !ClientEconomyData.teamName().isEmpty()) {
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.zovcapture.classes.reset"),
                    button -> {
                        PacketDistributor.sendToServer(new ClearClassPayload());
                        onClose();
                    }
            ).bounds(buttonX, topPos() + panelHeight - 30, buttonWidth, 22).build());
        }
    }

    private Component buildClassLabel(PlayerClass playerClass) {
        Component line = Component.empty()
                .append(playerClass.displayName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
                .append(playerClass.description().copy().withStyle(ChatFormatting.GRAY));

        if (playerClass == PlayerClass.CAPTAIN) {
            line = line.copy().append(
                    Component.translatable("gui.zovcapture.classes.captain_note")
                            .withStyle(ChatFormatting.GOLD)
            );
        }
        return line;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawHeader(graphics);

        String currentClass = ClientEconomyData.playerClassId();
        if (currentClass.isEmpty()) {
            drawInfoBar(
                    graphics,
                    topPos() + 24,
                    18,
                    Component.translatable("gui.zovcapture.classes.none"),
                    0xFFCC66
            );
        } else {
            PlayerClass.fromId(currentClass).ifPresentOrElse(
                    playerClass -> drawInfoBar(
                            graphics,
                            topPos() + 24,
                            18,
                            Component.translatable("gui.zovcapture.classes.current", playerClass.displayName()),
                            0x66CCFF
                    ),
                    () -> drawInfoBar(
                            graphics,
                            topPos() + 24,
                            18,
                            Component.translatable("gui.zovcapture.classes.none"),
                            0xFFCC66
                    )
            );
        }

        if (ClientEconomyData.teamName().isEmpty()) {
            drawMutedCentered(graphics, Component.translatable("gui.zovcapture.classes.need_team"), topPos() + 90);
        } else {
            drawMutedCentered(graphics, Component.translatable("gui.zovcapture.classes.hint"), topPos() + panelHeight - 18);
        }
    }
}
