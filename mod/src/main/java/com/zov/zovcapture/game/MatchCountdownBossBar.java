package com.zov.zovcapture.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import javax.annotation.Nullable;

public final class MatchCountdownBossBar {
    @Nullable
    private static ServerBossEvent bossBar;

    private MatchCountdownBossBar() {
    }

    public static void show(MinecraftServer server, int totalSeconds, int secondsRemaining) {
        if (bossBar == null) {
            bossBar = new ServerBossEvent(
                    Component.empty(),
                    BossEvent.BossBarColor.GREEN,
                    BossEvent.BossBarOverlay.NOTCHED_20
            );
        }
        update(totalSeconds, secondsRemaining);
        bossBar.setVisible(true);
        refreshPlayers(server);
    }

    public static void update(int totalSeconds, int secondsRemaining) {
        if (bossBar == null) {
            return;
        }
        float progress = totalSeconds <= 0 ? 0.0F : secondsRemaining / (float) totalSeconds;
        bossBar.setName(Component.translatable("bossbar.zovcapture.match.countdown", secondsRemaining));
        bossBar.setProgress(Math.clamp(progress, 0.0F, 1.0F));
    }

    public static void refreshPlayers(MinecraftServer server) {
        if (bossBar == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }
    }

    public static void clear() {
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar.setVisible(false);
        }
    }
}
