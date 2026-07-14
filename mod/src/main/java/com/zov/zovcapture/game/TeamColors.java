package com.zov.zovcapture.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;

public final class TeamColors {
    private TeamColors() {
    }

    public static BossEvent.BossBarColor toBossBarColor(@Nullable ChatFormatting formatting) {
        if (formatting == null) {
            return BossEvent.BossBarColor.WHITE;
        }
        return switch (formatting) {
            case BLACK -> BossEvent.BossBarColor.WHITE;
            case DARK_BLUE -> BossEvent.BossBarColor.BLUE;
            case DARK_GREEN -> BossEvent.BossBarColor.GREEN;
            case DARK_AQUA -> BossEvent.BossBarColor.BLUE;
            case DARK_RED -> BossEvent.BossBarColor.RED;
            case DARK_PURPLE -> BossEvent.BossBarColor.PURPLE;
            case GOLD -> BossEvent.BossBarColor.YELLOW;
            case GRAY -> BossEvent.BossBarColor.WHITE;
            case DARK_GRAY -> BossEvent.BossBarColor.WHITE;
            case BLUE -> BossEvent.BossBarColor.BLUE;
            case GREEN -> BossEvent.BossBarColor.GREEN;
            case AQUA -> BossEvent.BossBarColor.BLUE;
            case RED -> BossEvent.BossBarColor.RED;
            case LIGHT_PURPLE -> BossEvent.BossBarColor.PINK;
            case YELLOW -> BossEvent.BossBarColor.YELLOW;
            case WHITE -> BossEvent.BossBarColor.WHITE;
            default -> BossEvent.BossBarColor.WHITE;
        };
    }

    public static BossEvent.BossBarColor teamBossColor(@Nullable Team team) {
        if (team instanceof PlayerTeam playerTeam) {
            return toBossBarColor(playerTeam.getColor());
        }
        return BossEvent.BossBarColor.WHITE;
    }

    public static Component teamDisplayName(@Nullable Team team, String fallback) {
        if (team instanceof PlayerTeam playerTeam) {
            return playerTeam.getFormattedDisplayName();
        }
        if (team != null) {
            return Component.literal(team.getName());
        }
        return Component.literal(fallback);
    }

    @Nullable
    public static Team getPlayerTeam(Player player) {
        return player.getTeam();
    }
}
