package com.zov.zovcapture.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zov.zovcapture.airdrop.AirdropManager;
import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.game.CaptureGameManager;
import com.zov.zovcapture.game.TeamFriendlyFireRules;
import com.zov.zovcapture.network.CaptureNetworking;
import com.zov.zovcapture.presets.PresetStorage;
import com.zov.zovcapture.presets.ScoreboardTeamPresets;
import com.zov.zovcapture.shop.BalanceShopPreset;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.util.List;

public final class PresetCommands {
    private static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS = (context, builder) -> {
        try {
            return SharedSuggestionProvider.suggest(PresetStorage.listSavedPresets(), builder);
        } catch (IOException ignored) {
            return builder.buildFuture();
        }
    };

    private PresetCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> adminCommands() {
        return Commands.literal("preset")
                .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(PresetCommands::savePreset)))
                .then(Commands.literal("load")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(PRESET_SUGGESTIONS)
                                .executes(ctx -> loadPreset(ctx, false))
                                .then(Commands.literal("merge")
                                        .executes(ctx -> loadPreset(ctx, true)))))
                .then(Commands.literal("files")
                        .then(Commands.literal("list")
                                .executes(PresetCommands::listPresets)));
    }

    private static int savePreset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String rawName = StringArgumentType.getString(context, "name");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        try {
            var preset = data.exportPreset(source.getServer());
            PresetStorage.save(rawName, preset);
            int teamCount = preset.contains("ScoreboardTeams")
                    ? preset.getList("ScoreboardTeams", net.minecraft.nbt.Tag.TAG_COMPOUND).size()
                    : 0;
            final int savedTeams = teamCount;
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.preset.save.success",
                    rawName,
                    PresetStorage.presetDirectory().toString(),
                    savedTeams
            ), true);
            return 1;
        } catch (IOException | IllegalArgumentException exception) {
            source.sendFailure(Component.translatable("command.zovcapture.preset.save.failed", exception.getMessage()));
            return 0;
        }
    }

    private static int loadPreset(CommandContext<CommandSourceStack> context, boolean merge) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        String rawName = StringArgumentType.getString(context, "name");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        int shopCount = -1;
        int teamCount = 0;

        try {
            var preset = PresetStorage.load(rawName);
            if (preset.isEmpty()) {
                source.sendFailure(Component.translatable("command.zovcapture.preset.not_found", rawName));
                return 0;
            }
            AirdropManager.clearAirdrop(server, data, false);
            data.applyPresetConfiguration(preset, server.registryAccess(), merge);
            teamCount = ScoreboardTeamPresets.apply(server.getScoreboard(), preset, merge, server.registryAccess());
            TeamFriendlyFireRules.disableForAllTeams(server);
            if (!merge) {
                shopCount = BalanceShopPreset.applyTo(server, data);
            }
            CaptureGameManager.rebuildBossBars(server);
            CaptureNetworking.syncToAll(server);
        } catch (IOException | IllegalArgumentException exception) {
            source.sendFailure(Component.translatable("command.zovcapture.preset.load.failed", exception.getMessage()));
            return 0;
        }

        final int refreshedShopCount = shopCount;
        final int loadedTeams = teamCount;
        source.sendSuccess(() -> Component.translatable(
                merge ? "command.zovcapture.preset.load.merge.success" : "command.zovcapture.preset.load.success",
                rawName
        ), true);
        if (loadedTeams > 0) {
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.preset.load.teams",
                    loadedTeams
            ), false);
        }
        if (refreshedShopCount >= 0) {
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.shop.preset.auto_after_preset",
                    BalanceShopPreset.PRESET_ID,
                    refreshedShopCount
            ), false);
        }
        return 1;
    }

    private static int listPresets(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            List<String> files = PresetStorage.listSavedPresets();
            if (files.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("command.zovcapture.preset.files.empty"), false);
                return 1;
            }
            source.sendSuccess(() -> Component.translatable("command.zovcapture.preset.files.header"), false);
            for (String file : files) {
                source.sendSuccess(() -> Component.literal("- " + file), false);
            }
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.preset.files.path",
                    PresetStorage.presetDirectory().toString()
            ), false);
            return files.size();
        } catch (IOException exception) {
            source.sendFailure(Component.translatable("command.zovcapture.preset.load.failed", exception.getMessage()));
            return 0;
        }
    }
}
