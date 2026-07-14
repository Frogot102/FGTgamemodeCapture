package com.zov.zovcapture.client;



import com.zov.zovcapture.network.PostMatchPlayerStatsSync;

import com.zov.zovcapture.network.TeamScoreSync;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;

import net.minecraft.world.scores.PlayerTeam;

import net.minecraft.world.scores.Scoreboard;



import java.util.Comparator;

import java.util.List;



public final class MatchOverviewScreen extends AbstractZovScreen {

    private static final int FOOTER_BUTTON_HEIGHT = 22;
    private static final int FOOTER_BUTTON_GAP = 6;
    private static final int FOOTER_RESERVED = 52;

    private int statsScroll;



    public MatchOverviewScreen() {

        super(Component.translatable("gui.zovcapture.match.title"), 340, 260);

    }



    public static void open() {

        net.minecraft.client.Minecraft.getInstance().setScreen(new MatchOverviewScreen());

    }



    @Override

    protected void init() {

        super.init();

        int buttonWidth = (panelWidth - PANEL_MARGIN * 2 - FOOTER_BUTTON_GAP) / 2;

        int x = leftPos() + PANEL_MARGIN;

        int y = footerButtonY();

        addRenderableWidget(TelegramLinkButton.create(

                x,

                y,

                buttonWidth,

                Component.translatable("gui.zovcapture.feedback.dm"),

                () -> Util.getPlatform().openUri("https://t.me/frogot_1")

        ));

        addRenderableWidget(TelegramLinkButton.create(

                x + buttonWidth + FOOTER_BUTTON_GAP,

                y,

                buttonWidth,

                Component.translatable("gui.zovcapture.feedback.channel"),

                () -> Util.getPlatform().openUri("https://t.me/Rubickhouse1")

        ));

    }

    private int footerButtonY() {

        return topPos() + panelHeight - FOOTER_RESERVED + 8;

    }

    private int footerHintY() {

        return topPos() + panelHeight - 10;

    }



    @Override

    protected boolean shouldDrawCredit() {

        return true;

    }



    @Override

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {

        if (ClientCaptureData.matchFinished() && !ClientCaptureData.postMatchStats().isEmpty()) {

            int maxScroll = Math.max(0, ClientCaptureData.postMatchStats().size() * 11 - 72);

            statsScroll = (int) Math.max(0, Math.min(maxScroll, statsScroll - scrollY * 11));

            return true;

        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

    }



    @Override

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        super.render(graphics, mouseX, mouseY, partialTick);

        drawHeader(graphics);



        boolean finished = ClientCaptureData.matchFinished();
        boolean preparing = ClientCaptureData.matchPreparing();
        boolean active = ClientCaptureData.gameActive();
        Component statusText;
        int statusColor;
        if (finished) {
            statusText = Component.translatable(
                    "gui.zovcapture.match.finished",
                    resolveTeamLabel(
                            this.minecraft.level != null ? this.minecraft.level.getScoreboard() : null,
                            ClientCaptureData.winningTeam()
                    )
            );
            statusColor = 0xFFAA55;
        } else if (preparing) {
            statusText = Component.translatable(
                    "gui.zovcapture.match.preparing",
                    ClientCaptureData.countdownSeconds()
            );
            statusColor = 0x55AAFF;
        } else {
            statusText = Component.translatable(active ? "gui.zovcapture.match.active" : "gui.zovcapture.match.inactive");
            statusColor = active ? 0x55FF55 : 0xFF6666;
        }



        drawInfoBar(graphics, topPos() + 24, 18, statusText, statusColor);



        int x = leftPos() + PANEL_MARGIN + 2;

        int y = topPos() + 48;



        graphics.drawString(

                this.font,

                Component.translatable("gui.zovcapture.match.win_score", ClientCaptureData.pointsToWin()),

                x,

                y,

                0xE0E0E0,

                false

        );

        y += 14;



        graphics.drawString(this.font, Component.translatable("gui.zovcapture.match.scores"), x, y, 0xFFFFFF, true);

        y += 12;

        drawSeparator(graphics, y);

        y += 6;



        List<TeamScoreSync> scores = ClientCaptureData.teamScores().stream()

                .sorted(Comparator.comparingInt(TeamScoreSync::score).reversed().thenComparing(TeamScoreSync::team))

                .toList();



        Scoreboard scoreboard = this.minecraft.level != null ? this.minecraft.level.getScoreboard() : null;

        int pointsToWin = Math.max(1, ClientCaptureData.pointsToWin());



        if (scores.isEmpty()) {

            graphics.drawString(this.font, Component.translatable("gui.zovcapture.match.no_scores"), x, y, 0xA0A0A0, false);

            y += 14;

        } else {

            for (TeamScoreSync entry : scores) {

                Component teamLabel = resolveTeamLabel(scoreboard, entry.team());

                int color = teamColor(entry.team());

                int barWidth = panelWidth - PANEL_MARGIN * 2 - 4;

                int fillWidth = Math.min(barWidth, (int) ((entry.score() / (float) pointsToWin) * barWidth));



                graphics.fill(x, y + 10, x + barWidth, y + 16, 0xFF1A1A1A);

                if (fillWidth > 0) {

                    graphics.fill(x, y + 10, x + fillWidth, y + 16, color & 0xCCFFFFFF);

                }



                graphics.drawString(

                        this.font,

                        Component.translatable("gui.zovcapture.match.score_line", teamLabel, entry.score()),

                        x,

                        y,

                        color,

                        false

                );

                y += 20;

            }

        }



        y += 2;

        graphics.drawString(this.font, Component.translatable("gui.zovcapture.match.money_header"), x, y, 0xFFFFFF, true);

        y += 12;

        drawSeparator(graphics, y);

        y += 6;



        graphics.drawString(

                this.font,

                Component.translatable("gui.zovcapture.menu.personal", ClientEconomyData.personalMoney()),

                x,

                y,

                0x55FF55,

                false

        );

        y += 12;

        if (!ClientEconomyData.teamName().isEmpty()) {

            graphics.drawString(

                    this.font,

                    Component.translatable("gui.zovcapture.menu.team", ClientEconomyData.teamMoney()),

                    x,

                    y,

                    0x55AAFF,

                    false

            );

            y += 12;

        }



        if (finished && !ClientCaptureData.postMatchStats().isEmpty()) {

            y += 4;

            graphics.drawString(this.font, Component.translatable("gui.zovcapture.match.stats_header"), x, y, 0xFFFFFF, true);

            y += 12;

            drawSeparator(graphics, y);

            y += 4;



            int statsTop = y;

            int statsBottom = topPos() + panelHeight - FOOTER_RESERVED;

            graphics.enableScissor(x - 2, statsTop, x + panelWidth - PANEL_MARGIN * 2, statsBottom);



            graphics.drawString(this.font, Component.translatable("gui.zovcapture.match.stats.columns"), x, statsTop - statsScroll, 0xA0A0A0, false);

            int rowY = statsTop + 10 - statsScroll;



            for (PostMatchPlayerStatsSync stats : ClientCaptureData.postMatchStats()) {

                if (rowY + 10 >= statsTop && rowY <= statsBottom) {

                    int nameColor = stats.teamName().isEmpty() ? 0xDDDDDD : teamColor(stats.teamName());

                    graphics.drawString(

                            this.font,

                            Component.translatable(

                                    "gui.zovcapture.match.stats.line",

                                    stats.playerName(),

                                    stats.kills(),

                                    stats.deaths(),

                                    stats.captures(),

                                    stats.airdrops(),

                                    stats.moneyEarned()

                            ),

                            x,

                            rowY,

                            nameColor,

                            false

                    );

                }

                rowY += 11;

            }



            graphics.disableScissor();

        }



        drawMutedCentered(graphics, Component.translatable("gui.zovcapture.match.hint"), footerHintY());

    }



    private Component resolveTeamLabel(Scoreboard scoreboard, String teamName) {

        if (teamName == null || teamName.isEmpty()) {

            return Component.translatable("gui.zovcapture.match.no_winner");

        }

        if (scoreboard != null) {

            PlayerTeam team = scoreboard.getPlayerTeam(teamName);

            if (team != null) {

                return team.getFormattedDisplayName();

            }

        }

        return Component.literal(teamName);

    }

}

