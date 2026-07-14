package com.zov.zovcapture.airdrop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import javax.annotation.Nullable;

final class AirdropBossBar {
    @Nullable
    private static ServerBossEvent bossBar;

    private AirdropBossBar() {
    }

    static void begin(ServerPlayer player, String displayName) {
        if (bossBar == null) {
            bossBar = new ServerBossEvent(
                    Component.empty(),
                    BossEvent.BossBarColor.YELLOW,
                    BossEvent.BossBarOverlay.PROGRESS
            );
        }
        update(displayName, 0.0F);
        bossBar.setVisible(true);
        bossBar.addPlayer(player);
    }

    static void update(String displayName, float progress) {
        if (bossBar == null) {
            return;
        }
        int percent = Math.round(progress * 100.0F);
        bossBar.setName(Component.translatable("bossbar.zovcapture.airdrop.capture", displayName, percent));
        bossBar.setProgress(Math.clamp(progress, 0.0F, 1.0F));
    }

    static void end(ServerPlayer player) {
        if (bossBar == null) {
            return;
        }
        bossBar.removePlayer(player);
        if (bossBar.getPlayers().isEmpty()) {
            bossBar.setVisible(false);
        }
    }

    static void clear() {
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar.setVisible(false);
        }
    }
}
