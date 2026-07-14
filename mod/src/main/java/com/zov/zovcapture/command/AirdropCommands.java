package com.zov.zovcapture.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zov.zovcapture.airdrop.AirdropLootEntry;
import com.zov.zovcapture.airdrop.AirdropSpawnPoint;
import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.game.CaptureGameManager;
import com.zov.zovcapture.game.ParticleUtils;
import com.zov.zovcapture.network.AirdropAdminNetworking;
import com.zov.zovcapture.network.CaptureNetworking;
import com.zov.zovcapture.network.OpenAirdropAdminPayload;
import com.zov.zovcapture.shop.ShopBundleHelper;
import com.zov.zovcapture.shop.ShopManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

public final class AirdropCommands {
    private static final SuggestionProvider<CommandSourceStack> POINT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    CaptureGameData.get(context.getSource().getServer().overworld()).airdropSpawnPoints().keySet(),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> LOOT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    CaptureGameData.get(context.getSource().getServer().overworld()).airdropLoot().keySet(),
                    builder
            );

    private AirdropCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> adminCommands() {
        return Commands.literal("airdrop")
                .then(Commands.literal("menu")
                        .executes(AirdropCommands::openMenu))
                .then(Commands.literal("enable")
                        .executes(ctx -> setEnabled(ctx, true)))
                .then(Commands.literal("disable")
                        .executes(ctx -> setEnabled(ctx, false)))
                .then(Commands.literal("point")
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(3, 128))
                                                .executes(AirdropCommands::createPoint))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(POINT_SUGGESTIONS)
                                        .executes(AirdropCommands::removePoint)))
                        .then(Commands.literal("list")
                                .executes(AirdropCommands::listPoints))
                        .then(Commands.literal("setradius")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(POINT_SUGGESTIONS)
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(3, 128))
                                                .executes(AirdropCommands::setRadius)))))
                .then(Commands.literal("loot")
                        .then(Commands.literal("add")
                                .then(Commands.literal("item")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .then(Commands.argument("weight", IntegerArgumentType.integer(1, 100000))
                                                        .then(Commands.argument("item", StringArgumentType.string())
                                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                                        .executes(AirdropCommands::addItemLoot))))))
                                .then(Commands.literal("bundle")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .then(Commands.argument("weight", IntegerArgumentType.integer(1, 100000))
                                                        .executes(AirdropCommands::addBundleLoot)
                                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                                .executes(AirdropCommands::addBundleLoot)))))
                                .then(Commands.literal("personal")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .then(Commands.argument("weight", IntegerArgumentType.integer(1, 100000))
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                                                .executes(AirdropCommands::addPersonalMoneyLoot)))))
                                .then(Commands.literal("team")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .then(Commands.argument("weight", IntegerArgumentType.integer(1, 100000))
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                                                .executes(AirdropCommands::addTeamMoneyLoot))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .suggests(LOOT_SUGGESTIONS)
                                        .executes(AirdropCommands::removeLoot)))
                        .then(Commands.literal("list")
                                .executes(AirdropCommands::listLoot)))
                .then(Commands.literal("set")
                        .then(Commands.literal("interval")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(30, 86400))
                                        .executes(AirdropCommands::setInterval)))
                        .then(Commands.literal("capture")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 300))
                                        .executes(AirdropCommands::setCapture)))
                        .then(Commands.literal("particle")
                                .then(Commands.argument("particle", StringArgumentType.string())
                                        .suggests(CaptureCommands.PARTICLE_SUGGESTIONS)
                                        .executes(AirdropCommands::setParticle))));
    }

    private static int openMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PacketDistributor.sendToPlayer(player, new OpenAirdropAdminPayload());
        AirdropAdminNetworking.syncPlayer(player);
        context.getSource().sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.menu.opened"), true);
        return 1;
    }

    private static int setEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        CaptureGameData data = CaptureGameManager.getData(context.getSource().getLevel());
        data.setAirdropEnabled(enabled);
        CaptureNetworking.syncToAll(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable(
                enabled ? "command.zovcapture.airdrop.enabled" : "command.zovcapture.airdrop.disabled"
        ), true);
        return 1;
    }

    private static int createPoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        CaptureGameData data = CaptureGameManager.getData(level);

        String rawName = StringArgumentType.getString(context, "name");
        String id = CaptureGameManager.normalizeId(rawName);
        int radius = IntegerArgumentType.getInteger(context, "radius");

        if (data.getAirdropSpawnPoint(id) != null) {
            source.sendFailure(Component.translatable("command.zovcapture.airdrop.point.exists", rawName));
            return 0;
        }

        var position = source.getPosition();
        data.addAirdropSpawnPoint(new AirdropSpawnPoint(
                id,
                rawName,
                net.minecraft.core.BlockPos.containing(position),
                radius,
                level.dimension()
        ));
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.airdrop.point.create.success",
                rawName,
                radius
        ), true);
        return 1;
    }

    private static int removePoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawName = StringArgumentType.getString(context, "name");
        String id = CaptureGameManager.normalizeId(rawName);
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        if (!data.removeAirdropSpawnPoint(id)) {
            source.sendFailure(Component.translatable("command.zovcapture.airdrop.point.not_found", rawName));
            return 0;
        }
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.point.remove.success", rawName), true);
        return 1;
    }

    private static int listPoints(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        if (data.airdropSpawnPoints().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.point.list.empty"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.point.list.header"), false);
        for (AirdropSpawnPoint point : data.airdropSpawnPoints().values()) {
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.airdrop.point.list.entry",
                    point.displayName(),
                    point.radius(),
                    point.center().getX(),
                    point.center().getY(),
                    point.center().getZ()
            ), false);
        }
        return data.airdropSpawnPoints().size();
    }

    private static int setRadius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawName = StringArgumentType.getString(context, "name");
        String id = CaptureGameManager.normalizeId(rawName);
        int radius = IntegerArgumentType.getInteger(context, "radius");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        AirdropSpawnPoint point = data.getAirdropSpawnPoint(id);
        if (point == null) {
            source.sendFailure(Component.translatable("command.zovcapture.airdrop.point.not_found", rawName));
            return 0;
        }
        point.setRadius(radius);
        data.setDirty();
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.point.setradius.success", rawName, radius), true);
        return 1;
    }

    private static int addItemLoot(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawId = StringArgumentType.getString(context, "id");
        String id = ShopManager.normalizeId(rawId);
        int weight = IntegerArgumentType.getInteger(context, "weight");
        int count = IntegerArgumentType.getInteger(context, "count");
        String itemRaw = StringArgumentType.getString(context, "item");
        ResourceLocation itemId = parseItemId(itemRaw);
        if (itemId == null || !isValidItem(itemId)) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.invalid_item", itemRaw));
            return 0;
        }
        return addLoot(source, id, rawId, weight, AirdropLootEntry.LootType.ITEM, itemId.toString(), count, List.of());
    }

    private static int addBundleLoot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String rawId = StringArgumentType.getString(context, "id");
        String id = ShopManager.normalizeId(rawId);
        int weight = IntegerArgumentType.getInteger(context, "weight");
        List<ItemStack> items = ShopBundleHelper.captureInventory(player);
        if (items.isEmpty()) {
            source.sendFailure(Component.translatable("command.zovcapture.shop.bundle.empty"));
            return 0;
        }
        String name;
        try {
            name = StringArgumentType.getString(context, "name");
        } catch (IllegalArgumentException ignored) {
            name = rawId;
        }
        return addLoot(source, id, name, weight, AirdropLootEntry.LootType.BUNDLE, "", items.size(), items);
    }

    private static int addPersonalMoneyLoot(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawId = StringArgumentType.getString(context, "id");
        String id = ShopManager.normalizeId(rawId);
        int weight = IntegerArgumentType.getInteger(context, "weight");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        return addLoot(source, id, rawId, weight, AirdropLootEntry.LootType.PERSONAL_MONEY, "", amount, List.of());
    }

    private static int addTeamMoneyLoot(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawId = StringArgumentType.getString(context, "id");
        String id = ShopManager.normalizeId(rawId);
        int weight = IntegerArgumentType.getInteger(context, "weight");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        return addLoot(source, id, rawId, weight, AirdropLootEntry.LootType.TEAM_MONEY, "", amount, List.of());
    }

    private static int addLoot(
            CommandSourceStack source,
            String id,
            String displayName,
            int weight,
            AirdropLootEntry.LootType type,
            String payload,
            int count,
            List<ItemStack> bundleItems
    ) {
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        if (data.airdropLoot().containsKey(id)) {
            source.sendFailure(Component.translatable("command.zovcapture.airdrop.loot.exists", displayName));
            return 0;
        }
        data.addAirdropLoot(new AirdropLootEntry(id, displayName, weight, type, payload, count, bundleItems));
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.loot.add.success", displayName, weight), true);
        return 1;
    }

    private static int removeLoot(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawId = StringArgumentType.getString(context, "id");
        String id = ShopManager.normalizeId(rawId);
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        if (!data.removeAirdropLoot(id)) {
            source.sendFailure(Component.translatable("command.zovcapture.airdrop.loot.not_found", rawId));
            return 0;
        }
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.loot.remove.success", rawId), true);
        return 1;
    }

    private static int listLoot(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        if (data.airdropLoot().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.loot.list.empty"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.loot.list.header"), false);
        for (AirdropLootEntry entry : data.airdropLoot().values()) {
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.airdrop.loot.list.entry",
                    entry.displayName(),
                    entry.weight(),
                    entry.type().name(),
                    entry.count()
            ), false);
        }
        return data.airdropLoot().size();
    }

    private static int setInterval(CommandContext<CommandSourceStack> context) {
        CaptureGameData data = CaptureGameManager.getData(context.getSource().getLevel());
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        data.setAirdropIntervalSeconds(seconds);
        data.setNextAirdropTick(0);
        CaptureNetworking.syncToAll(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.set.interval", seconds), true);
        return 1;
    }

    private static int setCapture(CommandContext<CommandSourceStack> context) {
        CaptureGameData data = CaptureGameManager.getData(context.getSource().getLevel());
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        data.setAirdropCaptureSeconds(seconds);
        CaptureNetworking.syncToAll(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.set.capture", seconds), true);
        return 1;
    }

    private static int setParticle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String raw = StringArgumentType.getString(context, "particle");
        ResourceLocation particleId = ParticleUtils.parseParticleId(raw);
        if (!CaptureCommands.isSupportedParticle(particleId)) {
            source.sendFailure(Component.translatable("command.zovcapture.particle.invalid", raw));
            return 0;
        }
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.setAirdropParticle(particleId);
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.airdrop.set.particle", raw), true);
        return 1;
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
}
