package com.zov.zovcapture.shop;

import com.zov.zovcapture.economy.EconomyManager;
import com.zov.zovcapture.game.BaseShopAccess;
import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.game.MatchStatManager;
import com.zov.zovcapture.game.MatchStatRules;
import com.zov.zovcapture.game.PlayerClass;
import com.zov.zovcapture.game.PlayerClassManager;
import com.zov.zovcapture.network.EconomyNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;

public final class ShopManager {
    private ShopManager() {
    }

    public static boolean purchase(ServerPlayer player, String offerId) {
        CaptureGameData data = CaptureGameData.get(player.server.overworld());
        ShopOffer offer = data.getShopOffer(offerId);
        if (offer == null) {
            player.sendSystemMessage(Component.translatable("command.zovcapture.shop.not_found", offerId));
            return false;
        }

        PlayerClass playerClass = PlayerClassManager.getClass(data, player.getUUID()).orElse(null);
        Team team = player.getTeam();
        if (!ClassShopAccess.isOfferVisible(data, player, team, playerClass, offer)) {
            if (playerClass == null) {
                player.sendSystemMessage(Component.translatable("message.zovcapture.class.required"));
            } else {
                player.sendSystemMessage(Component.translatable("message.zovcapture.shop.class_denied"));
            }
            return false;
        }

        if (!BaseShopAccess.isOnTeamBase(player, data)) {
            player.sendSystemMessage(Component.translatable("message.zovcapture.shop.not_at_base"));
            return false;
        }

        if (offer.wallet() == ShopOffer.WalletType.TEAM) {
            if (team == null) {
                player.sendSystemMessage(Component.translatable("command.zovcapture.shop.no_team"));
                return false;
            }
            if (!EconomyManager.isCaptain(data, player)) {
                player.sendSystemMessage(Component.translatable("command.zovcapture.shop.not_captain"));
                return false;
            }
            if (!EconomyManager.spendTeam(data, team.getName(), offer.cost())) {
                player.sendSystemMessage(Component.translatable("command.zovcapture.shop.not_enough_team", offer.cost()));
                return false;
            }
        } else {
            if (!EconomyManager.spendPersonal(data, player.getUUID(), offer.cost())) {
                player.sendSystemMessage(Component.translatable("command.zovcapture.shop.not_enough_personal", offer.cost()));
                return false;
            }
        }

        if (!applyOffer(player, data, offer, team)) {
            refund(player, data, offer, team);
            return false;
        }

        player.sendSystemMessage(Component.translatable(
                "command.zovcapture.shop.buy.success",
                offer.displayName(),
                offer.cost()
        ));
        EconomyNetworking.syncPlayer(player);
        if (team != null && offer.wallet() == ShopOffer.WalletType.TEAM) {
            EconomyNetworking.syncTeam(team.getName(), player.server);
        }
        return true;
    }

    private static boolean applyOffer(ServerPlayer player, CaptureGameData data, ShopOffer offer, @Nullable Team team) {
        return switch (offer.type()) {
            case ITEM -> giveItem(player, offer);
            case BUNDLE -> giveBundle(player, offer);
            case TEAM_STAT -> applyTeamStat(player.server, data, offer, team);
            case COMMAND -> runCommand(player, offer);
        };
    }

    private static boolean giveItem(ServerPlayer player, ShopOffer offer) {
        ResourceLocation itemId = ResourceLocation.parse(offer.payload());
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemId)).orElse(Items.AIR);
        if (item == Items.AIR) {
            player.sendSystemMessage(Component.translatable("command.zovcapture.shop.invalid_item", itemId.toString()));
            return false;
        }
        ItemStack stack = new ItemStack(item, Math.max(1, offer.count()));
        return giveStack(player, stack);
    }

    private static boolean giveBundle(ServerPlayer player, ShopOffer offer) {
        if (offer.bundleItems().isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.zovcapture.shop.empty_bundle"));
            return false;
        }
        for (ItemStack stack : offer.bundleItems()) {
            if (!giveStack(player, stack.copy())) {
                return false;
            }
        }
        return true;
    }

    private static boolean giveStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        return true;
    }

    private static boolean applyTeamStat(
            net.minecraft.server.MinecraftServer server,
            CaptureGameData data,
            ShopOffer offer,
            @Nullable Team team
    ) {
        if (team == null) {
            return false;
        }

        String[] parts = offer.payload().split(":");
        if (parts.length != 2) {
            return false;
        }

        Optional<MatchStatRules.StatField> field = MatchStatRules.parseField(parts[0]);
        if (field.isEmpty()) {
            return false;
        }

        double value;
        try {
            value = Double.parseDouble(parts[1]);
        } catch (NumberFormatException ignored) {
            return false;
        }

        MatchStatManager.setRule(data, team.getName(), field.get(), value);
        for (ServerPlayer member : server.getPlayerList().getPlayers()) {
            if (member.getTeam() != null && team.getName().equals(member.getTeam().getName())) {
                MatchStatManager.revertPlayer(member);
                if (data.gameActive() && !data.matchFinished()) {
                    MatchStatManager.applyToPlayer(member, data);
                }
            }
        }
        return true;
    }

    private static boolean runCommand(ServerPlayer player, ShopOffer offer) {
        String command = offer.payload()
                .replace("{player}", player.getGameProfile().getName())
                .replace("{uuid}", player.getUUID().toString());
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        player.server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
        return true;
    }

    private static void refund(ServerPlayer player, CaptureGameData data, ShopOffer offer, @Nullable Team team) {
        if (offer.wallet() == ShopOffer.WalletType.TEAM && team != null) {
            data.addTeamMoney(team.getName(), offer.cost());
        } else {
            data.addPersonalMoney(player.getUUID(), offer.cost());
        }
    }

    public static String normalizeId(String raw) {
        return raw.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
