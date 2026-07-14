package com.zov.zovcapture.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.game.CaptureGameManager;
import com.zov.zovcapture.game.MatchStatRules;
import com.zov.zovcapture.network.CaptureNetworking;
import com.zov.zovcapture.shop.ShopBundleHelper;
import com.zov.zovcapture.shop.ShopCategory;
import com.zov.zovcapture.shop.ShopManager;
import com.zov.zovcapture.shop.ShopOffer;
import com.zov.zovcapture.shop.ShopStorage;
import com.zov.zovcapture.shop.BalanceShopPreset;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class ShopCommands {
    private static final SuggestionProvider<CommandSourceStack> SHOP_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    CaptureGameData.get(context.getSource().getServer().overworld()).shopOffers().keySet(),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> CATEGORY_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.asList("blocks", "tools", "weapons", "armor", "vehicles"),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> SAVED_SHOP_SUGGESTIONS = (context, builder) -> {
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>(BalanceShopPreset.listBuiltInPresets());
        try {
            names.addAll(ShopStorage.listSavedShops());
        } catch (IOException ignored) {
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(BalanceShopPreset.listBuiltInPresets(), builder);

    private ShopCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> adminCommands() {
        LiteralArgumentBuilder<CommandSourceStack> personal = Commands.literal("personal")
                .then(itemArgument(ShopCommands::addPersonalItem))
                .then(bundleArgument(ShopCommands::addPersonalBundle));

        LiteralArgumentBuilder<CommandSourceStack> team = Commands.literal("team")
                .then(itemArgument(ShopCommands::addTeamItem))
                .then(bundleArgument(ShopCommands::addTeamBundle))
                .then(Commands.literal("stat")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("cost", IntegerArgumentType.integer(1, 1000000))
                                        .then(Commands.argument("field", StringArgumentType.string())
                                                .suggests(CaptureCommands.STAT_FIELD_SUGGESTIONS)
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 10000.0))
                                                        .executes(ShopCommands::addTeamStat))))))
                .then(Commands.literal("command")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("cost", IntegerArgumentType.integer(1, 1000000))
                                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                                .executes(ShopCommands::addTeamCommand)))));

        return Commands.literal("shop")
                .then(Commands.literal("add").then(personal).then(team))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(SHOP_SUGGESTIONS)
                                .executes(ShopCommands::removeOffer)))
                .then(Commands.literal("list")
                        .executes(ShopCommands::listOffers))
                .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ShopCommands::saveShop)))
                .then(Commands.literal("load")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(SAVED_SHOP_SUGGESTIONS)
                                .executes(ctx -> loadShop(ctx, false))
                                .then(Commands.literal("merge")
                                        .executes(ctx -> loadShop(ctx, true)))))
                .then(Commands.literal("files")
                        .then(Commands.literal("list")
                                .executes(ShopCommands::listSavedShops)))
                .then(Commands.literal("preset")
                        .then(Commands.literal("list")
                                .executes(ShopCommands::listBuiltInPresets))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(PRESET_SUGGESTIONS)
                                .executes(ctx -> applyBuiltInPreset(ctx, false))
                                .then(Commands.literal("with_economy")
                                        .executes(ctx -> applyBuiltInPreset(ctx, true)))))
                .then(Commands.literal("reload")
                        .executes(ctx -> applyBuiltInPreset(ctx, false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> itemArgument(Command<CommandSourceStack> executor) {
        return Commands.literal("item")
                .then(
                        Commands.argument("id", StringArgumentType.string())
                                .then(
                                        Commands.argument("cost", IntegerArgumentType.integer(1, 1000000))
                                                .then(
                                                        Commands.argument("category", StringArgumentType.string())
                                                                .suggests(CATEGORY_SUGGESTIONS)
                                                                .then(
                                                                        Commands.argument("item", StringArgumentType.string())
                                                                                .then(
                                                                                        Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                                                                .executes(executor)
                                                                                )
                                                                )
                                                )
                                )
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> bundleArgument(Command<CommandSourceStack> executor) {
        return Commands.literal("bundle")
                .then(
                        Commands.argument("id", StringArgumentType.string())
                                .then(
                                        Commands.argument("cost", IntegerArgumentType.integer(1, 1000000))
                                                .then(
                                                        Commands.argument("category", StringArgumentType.string())
                                                                .suggests(CATEGORY_SUGGESTIONS)
                                                                .executes(executor)
                                                                .then(
                                                                        Commands.argument("name", StringArgumentType.greedyString())
                                                                                .executes(executor)
                                                                )
                                                )
                                )
                );
    }

    public static int buyOffer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = ShopManager.normalizeId(StringArgumentType.getString(context, "id"));
        return ShopManager.purchase(player, id) ? 1 : 0;
    }

    private static int addPersonalItem(CommandContext<CommandSourceStack> context) {
        return addItemOffer(context, ShopOffer.WalletType.PERSONAL);
    }

    private static int addTeamItem(CommandContext<CommandSourceStack> context) {
        return addItemOffer(context, ShopOffer.WalletType.TEAM);
    }

    private static int addItemOffer(CommandContext<CommandSourceStack> context, ShopOffer.WalletType wallet) {
        CommandSourceStack source = context.getSource();
        String rawId = StringArgumentType.getString(context, "id");
        String id = ShopManager.normalizeId(rawId);
        int cost = IntegerArgumentType.getInteger(context, "cost");
        int count = IntegerArgumentType.getInteger(context, "count");
        String itemRaw = StringArgumentType.getString(context, "item");
        ShopCategory category = parseCategory(source, context, "category");
        if (category == null) {
            return 0;
        }

        ResourceLocation itemId = parseItemId(itemRaw);
        if (itemId == null || !isValidItem(itemId)) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.invalid_item", itemRaw));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        if (data.getShopOffer(id) != null) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.exists", rawId));
            return 0;
        }

        data.addShopOffer(new ShopOffer(
                id,
                rawId,
                cost,
                wallet,
                ShopOffer.OfferType.ITEM,
                itemId.toString(),
                count,
                category,
                0,
                List.of()
        ));
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.shop.add.success", rawId, cost), true);
        return 1;
    }

    private static int addPersonalBundle(CommandContext<CommandSourceStack> context) {
        return addBundleOffer(context, ShopOffer.WalletType.PERSONAL);
    }

    private static int addTeamBundle(CommandContext<CommandSourceStack> context) {
        return addBundleOffer(context, ShopOffer.WalletType.TEAM);
    }

    private static int addBundleOffer(
            CommandContext<CommandSourceStack> context,
            ShopOffer.WalletType wallet
    ) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.bundle.player_only"));
            return 0;
        }

        String rawId = StringArgumentType.getString(context, "id");
        String id = ShopManager.normalizeId(rawId);
        int cost = IntegerArgumentType.getInteger(context, "cost");
        ShopCategory category = parseCategory(source, context, "category");
        if (category == null) {
            return 0;
        }

        List<ItemStack> items = ShopBundleHelper.captureInventory(player);
        if (items.isEmpty()) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.bundle.empty"));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        if (data.getShopOffer(id) != null) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.exists", rawId));
            return 0;
        }

        String name = readOptionalDisplayName(context, rawId);
        data.addShopOffer(new ShopOffer(
                id,
                name,
                cost,
                wallet,
                ShopOffer.OfferType.BUNDLE,
                "",
                items.size(),
                category,
                0,
                items
        ));
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.shop.bundle.add.success",
                name,
                cost,
                items.size()
        ), true);
        return 1;
    }

    private static int addTeamStat(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawId = StringArgumentType.getString(context, "id");
        String id = ShopManager.normalizeId(rawId);
        int cost = IntegerArgumentType.getInteger(context, "cost");
        String fieldRaw = StringArgumentType.getString(context, "field");
        double value = DoubleArgumentType.getDouble(context, "value");

        MatchStatRules.StatField field = MatchStatRules.parseField(fieldRaw).orElse(null);
        if (field == null) {
            source.sendFailure(Component.translatable("command.zovcapture.stats.field.invalid", fieldRaw));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        if (data.getShopOffer(id) != null) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.exists", rawId));
            return 0;
        }

        data.addShopOffer(new ShopOffer(
                id,
                rawId,
                cost,
                ShopOffer.WalletType.TEAM,
                ShopOffer.OfferType.TEAM_STAT,
                fieldRaw.toLowerCase() + ":" + value,
                1,
                ShopCategory.TOOLS,
                0,
                List.of()
        ));
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.shop.add.success", rawId, cost), true);
        return 1;
    }

    private static int addTeamCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawId = StringArgumentType.getString(context, "id");
        String id = ShopManager.normalizeId(rawId);
        int cost = IntegerArgumentType.getInteger(context, "cost");
        String command = StringArgumentType.getString(context, "command");

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        if (data.getShopOffer(id) != null) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.exists", rawId));
            return 0;
        }

        data.addShopOffer(new ShopOffer(
                id,
                rawId,
                cost,
                ShopOffer.WalletType.TEAM,
                ShopOffer.OfferType.COMMAND,
                command,
                1,
                ShopCategory.TOOLS,
                0,
                List.of()
        ));
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.shop.add.success", rawId, cost), true);
        return 1;
    }

    private static int removeOffer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String id = ShopManager.normalizeId(StringArgumentType.getString(context, "id"));
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        if (!data.removeShopOffer(id)) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.not_found", id));
            return 0;
        }

        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.shop.remove.success", id), true);
        return 1;
    }

    private static int listOffers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        if (data.shopOffers().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zovcapture.shop.list.empty"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("command.zovcapture.shop.list.header"), false);
        for (ShopOffer offer : data.shopOffers().values()) {
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.shop.list.entry",
                    offer.displayName(),
                    offer.cost(),
                    offer.wallet().name().toLowerCase(),
                    offer.category().name().toLowerCase(),
                    offer.type().name().toLowerCase(),
                    offer.type() == ShopOffer.OfferType.BUNDLE ? offer.bundleItems().size() : offer.payload(),
                    offer.count()
            ), false);
        }
        return data.shopOffers().size();
    }

    private static int saveShop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        try {
            ShopStorage.save(source.getServer(), name, data.shopOffers().values());
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.shop.save.success",
                    ShopStorage.normalizeFileName(name),
                    data.shopOffers().size()
            ), true);
            return 1;
        } catch (IllegalArgumentException | IOException exception) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.save.failed", exception.getMessage()));
            return 0;
        }
    }

    private static int loadShop(CommandContext<CommandSourceStack> context, boolean merge) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        try {
            String normalized = ShopStorage.normalizeFileName(name);
            List<ShopOffer> offers;
            boolean fromBuiltIn = false;

            if (BalanceShopPreset.isBuiltIn(normalized)) {
                offers = BalanceShopPreset.build(source.getServer()).orElse(List.of());
                fromBuiltIn = true;
            } else {
                offers = ShopStorage.load(source.getServer(), normalized);
                if (offers.isEmpty()) {
                    Optional<List<ShopOffer>> builtIn = BalanceShopPreset.resolve(source.getServer(), normalized);
                    if (builtIn.isPresent()) {
                        offers = builtIn.get();
                        fromBuiltIn = true;
                    }
                }
            }

            if (offers.isEmpty()) {
                source.sendFailure(Component.translatable("command.zovcapture.shop.load.not_found", name));
                return 0;
            }
            if (fromBuiltIn) {
                try {
                    ShopStorage.save(source.getServer(), normalized, offers);
                } catch (IOException exception) {
                    source.sendSuccess(() -> Component.translatable(
                            "command.zovcapture.shop.preset.save_failed",
                            exception.getMessage()
                    ), false);
                }
            }
            int count = merge ? data.mergeShopOffers(offers) : data.replaceShopOffers(offers);
            data.setDirty();
            CaptureNetworking.syncToAll(source.getServer());
            final boolean builtInPreset = fromBuiltIn;
            source.sendSuccess(() -> Component.translatable(
                    builtInPreset
                            ? "command.zovcapture.shop.load.builtin.success"
                            : merge ? "command.zovcapture.shop.load.merge.success" : "command.zovcapture.shop.load.success",
                    normalized,
                    count
            ), true);
            return 1;
        } catch (IllegalArgumentException | IOException exception) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.load.failed", exception.getMessage()));
            return 0;
        }
    }

    private static int applyBuiltInPreset(CommandContext<CommandSourceStack> context, boolean withEconomy) {
        CommandSourceStack source = context.getSource();
        String presetId = readPresetId(context);
        if (!BalanceShopPreset.PRESET_ID.equals(presetId)) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.preset.not_found", presetId));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        int count = BalanceShopPreset.applyTo(source.getServer(), data, withEconomy);
        if (count < 0) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.preset.failed"));
            return 0;
        }

        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable(
                withEconomy
                        ? "command.zovcapture.shop.preset.success_with_economy"
                        : "command.zovcapture.shop.preset.success",
                presetId,
                count
        ), true);
        return 1;
    }

    private static String readPresetId(CommandContext<CommandSourceStack> context) {
        try {
            return StringArgumentType.getString(context, "id").toLowerCase();
        } catch (IllegalArgumentException ignored) {
            return BalanceShopPreset.PRESET_ID;
        }
    }

    private static int listBuiltInPresets(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("command.zovcapture.shop.preset.list.header"), false);
        for (String preset : BalanceShopPreset.listBuiltInPresets()) {
            source.sendSuccess(() -> Component.literal("- " + preset), false);
        }
        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.shop.preset.hint",
                BalanceShopPreset.PRESET_ID
        ), false);
        return BalanceShopPreset.listBuiltInPresets().size();
    }

    private static int listSavedShops(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            List<String> files = ShopStorage.listSavedShops();
            if (files.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("command.zovcapture.shop.files.empty"), false);
                return 1;
            }
            source.sendSuccess(() -> Component.translatable("command.zovcapture.shop.files.header"), false);
            for (String file : files) {
                source.sendSuccess(() -> Component.literal("- " + file), false);
            }
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.shop.files.path",
                    ShopStorage.shopDirectory().toString()
            ), false);
            return files.size();
        } catch (IOException exception) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.load.failed", exception.getMessage()));
            return 0;
        }
    }

    @Nullable
    private static ShopCategory parseCategory(CommandSourceStack source, CommandContext<CommandSourceStack> context, String argument) {
        String raw = StringArgumentType.getString(context, argument);
        ShopCategory category = ShopCategory.parse(raw).orElse(null);
        if (category == null) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.invalid_category", raw));
        }
        return category;
    }

    @Nullable
    private static ResourceLocation parseItemId(String raw) {
        try {
            return raw.contains(":") ? ResourceLocation.parse(raw) : ResourceLocation.fromNamespaceAndPath("minecraft", raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isValidItem(ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemId)).orElse(Items.AIR);
        return item != Items.AIR;
    }

    private static String readOptionalDisplayName(CommandContext<CommandSourceStack> context, String fallback) {
        try {
            String name = StringArgumentType.getString(context, "name");
            return name != null && !name.isBlank() ? name : fallback;
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
