package com.zov.zovcapture.game;

import com.zov.zovcapture.network.EconomyNetworking;
import com.zov.zovcapture.util.ModItemHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;

/**
 * Match starter benefits with team-side pistols and optional class loadouts.
 */
public final class MatchStarterKit {
    public static final int START_PERSONAL_MONEY = 2000;
    public static final ResourceLocation DEFAULT_AMMO = ResourceLocation.fromNamespaceAndPath("superbwarfare", "handgun_ammo_box");
    public static final int DEFAULT_AMMO_COUNT = 1;

    private MatchStarterKit() {
    }

    public static void grantMatchStartAll(MinecraftServer server, CaptureGameData data) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            grantMatchStart(player, data);
        }
    }

    public static void grantMatchStart(ServerPlayer player, CaptureGameData data) {
        if (player.getTeam() == null) {
            return;
        }
        data.setPersonalMoney(player.getUUID(), START_PERSONAL_MONEY);
        giveRespawnLoadout(player, data.getPlayerClass(player.getUUID()));
        EconomyNetworking.syncPlayer(player);
        TeamFaction faction = TeamFaction.fromTeam(player.getTeam());
        player.sendSystemMessage(Component.translatable(
                "message.zovcapture.starter.match_start",
                START_PERSONAL_MONEY,
                faction.displayName()
        ));
    }

    public static void giveRespawnLoadout(ServerPlayer player) {
        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        giveRespawnLoadout(player, data.getPlayerClass(player.getUUID()));
    }

    public static void giveRespawnLoadout(ServerPlayer player, @Nullable String playerClassId) {
        Team team = player.getTeam();
        if (team == null) {
            return;
        }

        ResourceLocation weapon = TeamFaction.fromTeam(team).sidePistol();
        if (!ModItemHelper.giveItem(player, weapon, 1)) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.starter.missing_item", weapon));
        }
        if (!ModItemHelper.giveItem(player, DEFAULT_AMMO, DEFAULT_AMMO_COUNT)) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.starter.missing_item", DEFAULT_AMMO));
        }

        PlayerClass.fromId(playerClassId).ifPresent(playerClass -> giveClassExtras(player, playerClass));

        player.sendSystemMessage(Component.translatable(
                "message.zovcapture.starter.respawn_kit",
                TeamFaction.fromTeam(team).displayName()
        ));
    }

    private static void giveClassExtras(ServerPlayer player, PlayerClass playerClass) {
        switch (playerClass) {
            case ASSAULT -> ModItemHelper.giveItem(
                    player,
                    ResourceLocation.fromNamespaceAndPath("superbwarfare", "hand_grenade"),
                    1
            );
            case MEDIC -> ModItemHelper.giveItem(
                    player,
                    ResourceLocation.fromNamespaceAndPath("superbwarfare", "medical_kit"),
                    1
            );
            case ENGINEER -> ModItemHelper.giveItem(
                    player,
                    ResourceLocation.fromNamespaceAndPath("superbwarfare", "repair_tool"),
                    1
            );
            case SCOUT -> giveScoutStarterKit(player);
            case CAPTAIN -> ModItemHelper.giveItem(
                    player,
                    ResourceLocation.fromNamespaceAndPath("zovcapture", "siege_breaker"),
                    1
            );
            default -> {
            }
        }
    }

    public static boolean isEligible(CaptureGameData data) {
        return !data.matchFinished() && (data.gameActive() || data.isCountdownPending());
    }

    private static void giveScoutStarterKit(ServerPlayer player) {
        giveOptionalItem(player, "superbwarfare", "claymore_mine", 1);
    }

    private static void giveOptionalItem(ServerPlayer player, String namespace, String path, int count) {
        ModItemHelper.giveItem(
                player,
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                count
        );
    }
}
