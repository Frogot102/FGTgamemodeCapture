package com.zov.zovcapture.client;

import com.zov.zovcapture.ZovCaptureMod;
import com.zov.zovcapture.network.AirdropStateSync;
import com.zov.zovcapture.network.TeamScoreSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = ZovCaptureMod.MOD_ID, value = Dist.CLIENT)
public final class ScoreHudRenderer {
    private ScoreHudRenderer() {
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

        List<TeamScoreSync> scores = ClientCaptureData.teamScores();
        Scoreboard scoreboard = minecraft.level.getScoreboard();
        AirdropStateSync airdrop = ClientCaptureData.airdropState();
        boolean hasScores = !scores.isEmpty() || !scoreboard.getPlayerTeams().isEmpty();
        if (!hasScores && !airdrop.enabled()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 8;
        int y = 8;

        if (hasScores) {
            int winScore = ClientCaptureData.pointsToWin();
            List<TeamScoreSync> sorted;
            if (scores.isEmpty()) {
                sorted = scoreboard.getPlayerTeams().stream()
                        .map(team -> new TeamScoreSync(team.getName(), 0))
                        .sorted(Comparator.comparing(TeamScoreSync::team))
                        .toList();
            } else {
                sorted = scores.stream()
                        .sorted(Comparator.comparingInt(TeamScoreSync::score).reversed()
                                .thenComparing(TeamScoreSync::team))
                        .toList();
            }

            graphics.drawString(minecraft.font, Component.translatable("hud.zovcapture.title", winScore), x, y, 0xFFFFFF, true);
            y += 12;

            for (TeamScoreSync entry : sorted) {
                PlayerTeam team = scoreboard.getPlayerTeam(entry.team());
                Component line;
                int color = 0xFFFFFF;
                if (team != null) {
                    line = Component.translatable("hud.zovcapture.team_line", team.getFormattedDisplayName(), entry.score());
                    if (team.getColor() != null && team.getColor().getColor() != null) {
                        color = team.getColor().getColor() | 0xFF000000;
                    }
                } else {
                    line = Component.translatable("hud.zovcapture.team_line", entry.team(), entry.score());
                }
                graphics.drawString(minecraft.font, line, x, y, color, true);
                y += 10;
            }

            if (!ClientEconomyData.teamName().isEmpty()) {
                y += 2;
                graphics.drawString(
                        minecraft.font,
                        Component.translatable("hud.zovcapture.team_money", ClientEconomyData.teamMoney()),
                        x,
                        y,
                        0x55AAFF,
                        true
                );
                y += 10;
                if (ClientEconomyData.activeTeamMoneyPulse().isPresent()) {
                    graphics.drawString(
                            minecraft.font,
                            Component.translatable(
                                    "hud.zovcapture.team_money_gain",
                                    ClientEconomyData.activeTeamMoneyPulse().getAsInt()
                            ),
                            x,
                            y,
                            0x66FF66,
                            true
                    );
                    y += 10;
                }
            }
        }

        if (airdrop.enabled()) {
            if (hasScores) {
                y += 4;
            }
            if (airdrop.active()) {
                graphics.drawString(
                        minecraft.font,
                        Component.translatable("hud.zovcapture.airdrop.active", airdrop.displayName()),
                        x,
                        y,
                        0xFFFFAA55,
                        true
                );
            } else if (airdrop.secondsUntilNext() >= 0) {
                int minutes = airdrop.secondsUntilNext() / 60;
                int seconds = airdrop.secondsUntilNext() % 60;
                graphics.drawString(
                        minecraft.font,
                        Component.translatable("hud.zovcapture.airdrop.timer", minutes, seconds),
                        x,
                        y,
                        0xFF88CCFF,
                        true
                );
            }
        }
    }
}
