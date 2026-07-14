package com.zov.zovcapture.client;

import com.zov.zovcapture.network.JoinTeamPayload;
import com.zov.zovcapture.network.LeaveTeamPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TeamSelectScreen extends AbstractZovScreen {
    private List<PlayerTeam> teams = List.of();

    public TeamSelectScreen() {
        super(Component.translatable("gui.zovcapture.teams.title"), 200, 210);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new TeamSelectScreen());
    }

    @Override
    protected void init() {
        super.init();
        refreshTeams();

        int buttonX = leftPos() + PANEL_MARGIN;
        int buttonWidth = panelWidth - PANEL_MARGIN * 2;
        int buttonY = topPos() + 52;

        for (PlayerTeam team : teams) {
            int members = team.getPlayers().size();
            Component label = Component.empty()
                    .append(team.getFormattedDisplayName())
                    .append(Component.literal("  "))
                    .append(Component.translatable("gui.zovcapture.teams.members_short", members)
                            .withStyle(ChatFormatting.GRAY));

            addRenderableWidget(Button.builder(label, button -> {
                PacketDistributor.sendToServer(new JoinTeamPayload(team.getName()));
                onClose();
            }).bounds(buttonX, buttonY, buttonWidth, 22).build());

            buttonY += 24;
        }

        if (!ClientEconomyData.teamName().isEmpty()) {
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.zovcapture.teams.leave"),
                    button -> {
                        PacketDistributor.sendToServer(new LeaveTeamPayload());
                        onClose();
                    }
            ).bounds(buttonX, topPos() + panelHeight - 30, buttonWidth, 22).build());
        }
    }

    private void refreshTeams() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            teams = List.of();
            return;
        }

        Scoreboard scoreboard = level.getScoreboard();
        List<PlayerTeam> loaded = new ArrayList<>();
        for (PlayerTeam team : scoreboard.getPlayerTeams()) {
            loaded.add(team);
        }
        loaded.sort(Comparator.comparing(PlayerTeam::getName));
        teams = List.copyOf(loaded);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawHeader(graphics);

        String currentTeam = ClientEconomyData.teamName();
        if (currentTeam.isEmpty()) {
            drawInfoBar(
                    graphics,
                    topPos() + 24,
                    18,
                    Component.translatable("gui.zovcapture.teams.no_team"),
                    0xFFCC66
            );
        } else {
            Component currentLabel = Component.translatable("gui.zovcapture.teams.current", currentTeam);
            if (this.minecraft.level != null) {
                PlayerTeam team = this.minecraft.level.getScoreboard().getPlayerTeam(currentTeam);
                if (team != null) {
                    currentLabel = Component.translatable("gui.zovcapture.teams.current_named")
                            .append(team.getFormattedDisplayName());
                }
            }
            drawInfoBar(graphics, topPos() + 24, 18, currentLabel, teamColor(currentTeam));
        }

        if (teams.isEmpty()) {
            drawMutedCentered(graphics, Component.translatable("gui.zovcapture.teams.empty"), topPos() + 90);
        } else {
            drawMutedCentered(graphics, Component.translatable("gui.zovcapture.teams.hint"), topPos() + panelHeight - 18);
        }
    }
}
