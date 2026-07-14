package com.zov.zovcapture.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zov.zovcapture.ZovCaptureConfig;
import com.zov.zovcapture.game.BaseZone;
import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.game.CaptureGameManager;
import com.zov.zovcapture.game.CapturePoint;
import com.zov.zovcapture.game.MatchLifecycle;
import com.zov.zovcapture.game.MatchRulesManager;
import com.zov.zovcapture.game.MatchStatManager;
import com.zov.zovcapture.game.MatchStartManager;
import com.zov.zovcapture.game.MatchStatRules;
import com.zov.zovcapture.game.ParticleUtils;
import com.zov.zovcapture.game.TeamVisualSettings;
import com.zov.zovcapture.item.ZovCaptureItems;
import com.zov.zovcapture.network.OpenMenuPayload;
import com.zov.zovcapture.network.CaptureNetworking;
import com.zov.zovcapture.network.EconomyNetworking;
import com.zov.zovcapture.airdrop.AirdropManager;
import com.zov.zovcapture.economy.EconomyManager;
import com.zov.zovcapture.economy.EconomyRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;

public final class CaptureCommands {
    private static final SuggestionProvider<CommandSourceStack> POINT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    CaptureGameData.get(context.getSource().getServer().overworld()).points().keySet(),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> BASE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    CaptureGameData.get(context.getSource().getServer().overworld()).baseZones().keySet(),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> SHOP_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    CaptureGameData.get(context.getSource().getServer().overworld()).shopOffers().keySet(),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> TEAM_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    context.getSource().getServer().getScoreboard().getTeamNames(),
                    builder
            );

    static final SuggestionProvider<CommandSourceStack> PARTICLE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.asList(
                            "redstone", "dust", "flame", "soul_fire_flame", "end_rod", "enchant", "happy_villager",
                            "witch", "electric_spark", "glow", "heart", "note", "cloud", "smoke",
                            "large_smoke", "portal", "crit", "splash", "bubble", "snowflake", "scrape",
                            "dragon_breath", "firework", "cherry_leaves", "falling_dust"
                    ),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> BOSS_COLOR_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.asList("pink", "blue", "red", "green", "yellow", "purple", "white"),
                    builder
            );

    static final SuggestionProvider<CommandSourceStack> STAT_FIELD_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.asList("maxhealth", "speed", "damage", "armor", "attackspeed", "knockback"),
                    builder
            );

    private CaptureCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("FGTgmC")
                        .then(Commands.literal("money")
                                .requires(source -> source.hasPermission(0))
                                .executes(CaptureCommands::showMoney))
                        .then(Commands.literal("menu")
                                .requires(source -> source.hasPermission(0))
                                .executes(CaptureCommands::openMenu))
                        .then(Commands.literal("shop")
                                .then(Commands.literal("buy")
                                        .requires(source -> source.hasPermission(0))
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .suggests(SHOP_SUGGESTIONS)
                                                .executes(ShopCommands::buyOffer))))
        );

        dispatcher.register(adminCommands());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adminCommands() {
        return Commands.literal("FGTgmC")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(3, 128))
                                        .executes(CaptureCommands::createPoint))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(POINT_SUGGESTIONS)
                                .executes(CaptureCommands::removePoint)))
                .then(Commands.literal("list")
                        .executes(CaptureCommands::listPoints))
                .then(Commands.literal("setradius")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(POINT_SUGGESTIONS)
                                .then(Commands.argument("radius", IntegerArgumentType.integer(3, 128))
                                        .executes(CaptureCommands::setRadius))))
                .then(Commands.literal("setwinscore")
                        .then(Commands.argument("score", IntegerArgumentType.integer(1, 10000))
                                .executes(CaptureCommands::setWinScore)))
                .then(Commands.literal("score")
                        .executes(CaptureCommands::showScores))
                .then(Commands.literal("reset")
                        .executes(CaptureCommands::resetMatch))
                .then(Commands.literal("start")
                        .executes(CaptureCommands::startMatch)
                        .then(Commands.literal("now")
                                .executes(CaptureCommands::startMatchInstant)))
                .then(Commands.literal("stop")
                        .executes(CaptureCommands::stopMatch))
                .then(Commands.literal("setneutralparticle")
                        .then(Commands.argument("particle", StringArgumentType.string())
                                .suggests(PARTICLE_SUGGESTIONS)
                                .executes(CaptureCommands::setNeutralParticle)))
                .then(teamCommands())
                .then(baseCommands())
                .then(captainCommands())
                .then(economyCommands())
                .then(ShopCommands.adminCommands())
                .then(AirdropCommands.adminCommands())
                .then(PresetCommands.adminCommands())
                .then(toolCommands())
                .then(statsCommands());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> teamCommands() {
        return Commands.literal("team")
                .then(Commands.literal("setparticle")
                        .then(Commands.argument("team", StringArgumentType.string())
                                .suggests(TEAM_SUGGESTIONS)
                                .then(Commands.argument("particle", StringArgumentType.string())
                                        .suggests(PARTICLE_SUGGESTIONS)
                                        .executes(CaptureCommands::setTeamParticle))))
                .then(Commands.literal("setbosscolor")
                        .then(Commands.argument("team", StringArgumentType.string())
                                .suggests(TEAM_SUGGESTIONS)
                                .then(Commands.argument("color", StringArgumentType.string())
                                        .suggests(BOSS_COLOR_SUGGESTIONS)
                                        .executes(CaptureCommands::setTeamBossColor))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("team", StringArgumentType.string())
                                .suggests(TEAM_SUGGESTIONS)
                                .executes(CaptureCommands::clearTeamVisuals)))
                .then(Commands.literal("list")
                        .executes(CaptureCommands::listTeamVisuals));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> baseCommands() {
        return Commands.literal("base")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(3))
                                        .executes(ctx -> createBase(ctx, null))
                                        .then(Commands.argument("team", StringArgumentType.string())
                                                .suggests(TEAM_SUGGESTIONS)
                                                .executes(ctx -> createBase(
                                                        ctx,
                                                        StringArgumentType.getString(ctx, "team")
                                                ))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(BASE_SUGGESTIONS)
                                .executes(CaptureCommands::removeBase)))
                .then(Commands.literal("list")
                        .executes(CaptureCommands::listBases))
                .then(Commands.literal("setradius")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(BASE_SUGGESTIONS)
                                .then(Commands.argument("radius", IntegerArgumentType.integer(3))
                                        .executes(CaptureCommands::setBaseRadius))))
                .then(Commands.literal("particles")
                        .then(Commands.literal("on")
                                .executes(ctx -> setBaseParticles(ctx, true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> setBaseParticles(ctx, false))))
                .then(Commands.literal("setparticle")
                        .then(Commands.argument("particle", StringArgumentType.string())
                                .suggests(PARTICLE_SUGGESTIONS)
                                .executes(CaptureCommands::setBaseParticle)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> captainCommands() {
        return Commands.literal("captain")
                .then(Commands.literal("set")
                        .then(Commands.argument("team", StringArgumentType.string())
                                .suggests(TEAM_SUGGESTIONS)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CaptureCommands::setCaptain))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("team", StringArgumentType.string())
                                .suggests(TEAM_SUGGESTIONS)
                                .executes(CaptureCommands::clearCaptain)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> economyCommands() {
        return Commands.literal("economy")
                .then(Commands.literal("set")
                        .then(Commands.literal("kill")
                                .then(Commands.literal("personal")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1000000))
                                                .executes(ctx -> setKillReward(ctx, true))))
                                .then(Commands.literal("team")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1000000))
                                                .executes(ctx -> setKillReward(ctx, false)))))
                        .then(Commands.literal("hold")
                                .then(Commands.literal("personal")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1000000))
                                                .executes(ctx -> setHoldReward(ctx, true))))
                                .then(Commands.literal("team")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1000000))
                                                .executes(ctx -> setHoldReward(ctx, false))))
                                .then(Commands.literal("mode")
                                        .then(Commands.literal("personal")
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> setHoldPersonalMode(ctx, EconomyRules.HoldPersonalMode.ALL_MEMBERS)))
                                                .then(Commands.literal("zone")
                                                        .executes(ctx -> setHoldPersonalMode(ctx, EconomyRules.HoldPersonalMode.IN_ZONE)))
                                                .then(Commands.literal("captain")
                                                        .executes(ctx -> setHoldPersonalMode(ctx, EconomyRules.HoldPersonalMode.CAPTAIN)))))
                                .then(Commands.literal("interval")
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                                .executes(CaptureCommands::setHoldInterval)))))
                .then(Commands.literal("give")
                        .then(Commands.literal("personal")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                                .executes(CaptureCommands::givePersonalMoney))))
                        .then(Commands.literal("team")
                                .then(Commands.argument("team", StringArgumentType.string())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                                .executes(CaptureCommands::giveTeamMoney)))))
                .then(Commands.literal("take")
                        .then(Commands.literal("personal")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                                .executes(CaptureCommands::takePersonalMoney))))
                        .then(Commands.literal("team")
                                .then(Commands.argument("team", StringArgumentType.string())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                                .executes(CaptureCommands::takeTeamMoney)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toolCommands() {
        return Commands.literal("tool")
                .then(Commands.literal("give")
                        .executes(ctx -> giveTool(ctx, null, 1))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> giveTool(ctx, EntityArgument.getPlayer(ctx, "player"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> giveTool(
                                                ctx,
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "count")
                                        ))))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> giveTool(
                                        ctx,
                                        null,
                                        IntegerArgumentType.getInteger(ctx, "count")
                                ))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> statsCommands() {
        return Commands.literal("stats")
                .then(Commands.literal("set")
                        .then(Commands.literal("all")
                                .then(Commands.argument("field", StringArgumentType.string())
                                        .suggests(STAT_FIELD_SUGGESTIONS)
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 10000.0))
                                                .executes(ctx -> setStat(ctx, null)))))
                        .then(Commands.argument("team", StringArgumentType.string())
                                .suggests(TEAM_SUGGESTIONS)
                                .then(Commands.argument("field", StringArgumentType.string())
                                        .suggests(STAT_FIELD_SUGGESTIONS)
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 10000.0))
                                                .executes(ctx -> setStat(
                                                        ctx,
                                                        StringArgumentType.getString(ctx, "team")
                                                ))))))
                .then(Commands.literal("clear")
                        .executes(ctx -> clearStats(ctx, null))
                        .then(Commands.argument("team", StringArgumentType.string())
                                .suggests(TEAM_SUGGESTIONS)
                                .executes(ctx -> clearStats(ctx, StringArgumentType.getString(ctx, "team")))))
                .then(Commands.literal("apply")
                        .executes(CaptureCommands::applyStats))
                .then(Commands.literal("list")
                        .executes(CaptureCommands::listStats));
    }

    private static int createPoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        CaptureGameData data = CaptureGameManager.getData(level);

        String rawName = StringArgumentType.getString(context, "name");
        String id = CaptureGameManager.normalizeId(rawName);
        int radius = IntegerArgumentType.getInteger(context, "radius");

        if (data.getPoint(id) != null) {
            source.sendFailure(Component.translatable("command.zovcapture.create.exists", rawName));
            return 0;
        }

        var position = source.getPosition();
        CapturePoint point = new CapturePoint(
                id,
                rawName,
                new BlockPos(
                        (int) Math.floor(position.x),
                        (int) Math.floor(position.y),
                        (int) Math.floor(position.z)
                ),
                radius,
                level.dimension()
        );
        data.addPoint(point);
        CaptureGameManager.rebuildBossBars(source.getServer());
        CaptureNetworking.syncToAll(source.getServer());

        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.create.success",
                rawName,
                radius,
                point.center().getX(),
                point.center().getY(),
                point.center().getZ()
        ), true);
        return 1;
    }

    private static int removePoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        String id = CaptureGameManager.normalizeId(StringArgumentType.getString(context, "name"));

        if (!data.removePoint(id)) {
            source.sendFailure(Component.translatable("command.zovcapture.not_found", id));
            return 0;
        }

        CaptureGameManager.removeBossBar(id);
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.remove.success", id), true);
        return 1;
    }

    private static int listPoints(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        if (data.points().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zovcapture.list.empty"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("command.zovcapture.list.header"), false);
        for (CapturePoint point : data.points().values()) {
            CapturePoint listedPoint = point;
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.list.entry",
                    listedPoint.displayName(),
                    listedPoint.radius(),
                    listedPoint.center().getX(),
                    listedPoint.center().getY(),
                    listedPoint.center().getZ(),
                    listedPoint.dimension().location().toString()
            ), false);
        }
        return data.points().size();
    }

    private static int setRadius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        String id = CaptureGameManager.normalizeId(StringArgumentType.getString(context, "name"));
        int radius = IntegerArgumentType.getInteger(context, "radius");

        CapturePoint point = data.getPoint(id);
        if (point == null) {
            source.sendFailure(Component.translatable("command.zovcapture.not_found", id));
            return 0;
        }

        point.setRadius(radius);
        data.setDirty();
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.setradius.success", point.displayName(), radius), true);
        return 1;
    }

    private static int setWinScore(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        int score = IntegerArgumentType.getInteger(context, "score");
        data.setPointsToWin(score);
        source.sendSuccess(() -> Component.translatable("command.zovcapture.setwinscore.success", score), true);
        return 1;
    }

    private static int showScores(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.score.header",
                data.pointsToWin()
        ), false);

        if (data.teamScores().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zovcapture.score.empty"), false);
            return 1;
        }

        for (Map.Entry<String, Integer> entry : data.teamScores().entrySet()) {
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.score.entry",
                    entry.getKey(),
                    entry.getValue()
            ), false);
        }
        return data.teamScores().size();
    }

    private static int resetMatch(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        AirdropManager.clearAirdrop(source.getServer(), data, false);
        MatchStartManager.cancelPreparation(source.getServer(), data);
        MatchLifecycle.onMatchEnd(source.getServer(), true);
        data.resetMatch();
        CaptureGameManager.rebuildBossBars(source.getServer());
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.reset.success"), true);
        return 1;
    }

    private static int startMatch(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        if (data.matchFinished()) {
            source.sendFailure(Component.translatable("command.zovcapture.start.finished"));
            return 0;
        }
        if (data.gameActive()) {
            source.sendFailure(Component.translatable("command.zovcapture.start.already_active"));
            return 0;
        }
        if (data.isCountdownPending()) {
            source.sendFailure(Component.translatable("command.zovcapture.start.already_preparing"));
            return 0;
        }

        MatchStartManager.beginPreparation(server, data);
        source.sendSuccess(() -> Component.translatable("command.zovcapture.start.preparing", MatchStartManager.COUNTDOWN_SECONDS), true);
        return 1;
    }

    private static int startMatchInstant(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        if (data.matchFinished()) {
            source.sendFailure(Component.translatable("command.zovcapture.start.finished"));
            return 0;
        }
        if (data.gameActive()) {
            source.sendFailure(Component.translatable("command.zovcapture.start.already_active"));
            return 0;
        }

        MatchStartManager.beginInstant(server, data);
        source.sendSuccess(() -> Component.translatable("command.zovcapture.start.now.success"), true);
        return 1;
    }

    private static int stopMatch(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        AirdropManager.clearAirdrop(source.getServer(), data, false);
        MatchStartManager.cancelPreparation(source.getServer(), data);
        data.setGameActive(false);
        MatchRulesManager.restoreMatchRules(source.getServer());
        data.setDirty();
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.stop.success"), true);
        return 1;
    }

    private static int setNeutralParticle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ResourceLocation particleId = TeamVisualSettings.parseParticleId(StringArgumentType.getString(context, "particle"));
        if (!isSupportedParticle(particleId)) {
            source.sendFailure(Component.translatable("command.zovcapture.particle.invalid", particleId.toString()));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.setNeutralParticle(particleId);
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.neutralparticle.success", particleId.toString()), true);
        return 1;
    }

    private static int setTeamParticle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String team = StringArgumentType.getString(context, "team");
        ResourceLocation particleId = TeamVisualSettings.parseParticleId(StringArgumentType.getString(context, "particle"));
        if (!isSupportedParticle(particleId)) {
            source.sendFailure(Component.translatable("command.zovcapture.particle.invalid", particleId.toString()));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.getOrCreateTeamVisual(team).setParticle(particleId);
        data.setDirty();
        CaptureGameManager.rebuildBossBars(source.getServer());
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.team.particle.success", team, particleId.toString()), true);
        return 1;
    }

    private static int setTeamBossColor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String team = StringArgumentType.getString(context, "team");
        String colorRaw = StringArgumentType.getString(context, "color");
        BossEvent.BossBarColor color = TeamVisualSettings.parseBossColor(colorRaw)
                .orElse(null);
        if (color == null) {
            source.sendFailure(Component.translatable(
                    "command.zovcapture.bosscolor.invalid",
                    colorRaw,
                    TeamVisualSettings.bossColorSuggestions()
            ));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.getOrCreateTeamVisual(team).setBossBarColor(color);
        data.setDirty();
        CaptureGameManager.rebuildBossBars(source.getServer());
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.team.bosscolor.success", team, color.getName()), true);
        return 1;
    }

    private static int clearTeamVisuals(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String team = StringArgumentType.getString(context, "team");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.clearTeamVisual(team);
        CaptureGameManager.rebuildBossBars(source.getServer());
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.team.clear.success", team), true);
        return 1;
    }

    private static int listTeamVisuals(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.team.list.neutral",
                data.neutralParticle().toString()
        ), false);

        if (data.teamVisuals().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zovcapture.team.list.empty"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("command.zovcapture.team.list.header"), false);
        for (Map.Entry<String, TeamVisualSettings> entry : data.teamVisuals().entrySet()) {
            TeamVisualSettings settings = entry.getValue();
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.team.list.entry",
                    entry.getKey(),
                    settings.particle().map(ResourceLocation::toString).orElse("-"),
                    settings.bossBarColor().map(BossEvent.BossBarColor::getName).orElse("-")
            ), false);
        }
        return data.teamVisuals().size();
    }

    private static int createBase(CommandContext<CommandSourceStack> context, @Nullable String team) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        CaptureGameData data = CaptureGameManager.getData(level);

        String rawName = StringArgumentType.getString(context, "name");
        String id = CaptureGameManager.normalizeId(rawName);
        int radius = IntegerArgumentType.getInteger(context, "radius");
        int maxRadius = ZovCaptureConfig.MAX_BASE_ZONE_RADIUS.get();

        if (radius > maxRadius) {
            source.sendFailure(Component.translatable("command.zovcapture.base.radius.too_large", maxRadius));
            return 0;
        }

        if (team != null && source.getServer().getScoreboard().getPlayerTeam(team) == null) {
            source.sendFailure(Component.translatable("command.zovcapture.base.team.not_found", team));
            return 0;
        }

        if (data.getBaseZone(id) != null) {
            source.sendFailure(Component.translatable("command.zovcapture.base.create.exists", rawName));
            return 0;
        }

        var position = source.getPosition();
        BlockPos center = new BlockPos(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y),
                (int) Math.floor(position.z)
        );

        BaseZone zone = new BaseZone(id, rawName, center, radius, level.dimension(), team);
        data.addBaseZone(zone);
        CaptureNetworking.syncToAll(source.getServer());

        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.base.create.success",
                rawName,
                radius,
                center.getX(),
                center.getY(),
                center.getZ(),
                team != null ? team : "-"
        ), true);
        return 1;
    }

    private static int removeBase(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        String id = CaptureGameManager.normalizeId(StringArgumentType.getString(context, "name"));

        if (!data.removeBaseZone(id)) {
            source.sendFailure(Component.translatable("command.zovcapture.base.not_found", id));
            return 0;
        }

        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.base.remove.success", id), true);
        return 1;
    }

    private static int listBases(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        if (data.baseZones().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zovcapture.base.list.empty"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("command.zovcapture.base.list.header"), false);
        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.base.list.particles",
                data.baseZoneParticlesEnabled()
                        ? Component.translatable("command.zovcapture.common.yes").getString()
                        : Component.translatable("command.zovcapture.common.no").getString(),
                data.baseZoneParticle().toString()
        ), false);
        for (BaseZone zone : data.baseZones().values()) {
            source.sendSuccess(() -> Component.translatable(
                    "command.zovcapture.base.list.entry",
                    zone.displayName(),
                    zone.radius(),
                    zone.team() != null ? zone.team() : "-",
                    zone.center().getX(),
                    zone.center().getY(),
                    zone.center().getZ(),
                    zone.dimension().location().toString()
            ), false);
        }
        return data.baseZones().size();
    }

    private static int setBaseRadius(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        String id = CaptureGameManager.normalizeId(StringArgumentType.getString(context, "name"));
        int radius = IntegerArgumentType.getInteger(context, "radius");
        int maxRadius = ZovCaptureConfig.MAX_BASE_ZONE_RADIUS.get();

        if (radius > maxRadius) {
            source.sendFailure(Component.translatable("command.zovcapture.base.radius.too_large", maxRadius));
            return 0;
        }

        BaseZone zone = data.getBaseZone(id);
        if (zone == null) {
            source.sendFailure(Component.translatable("command.zovcapture.base.not_found", id));
            return 0;
        }

        zone.setRadius(radius);
        data.setDirty();
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.base.setradius.success", zone.displayName(), radius), true);
        return 1;
    }

    private static int setBaseParticles(CommandContext<CommandSourceStack> context, boolean enabled) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.setBaseZoneParticlesEnabled(enabled);
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable(
                enabled
                        ? "command.zovcapture.base.particles.on"
                        : "command.zovcapture.base.particles.off"
        ), true);
        return 1;
    }

    private static int setBaseParticle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ResourceLocation particleId = TeamVisualSettings.parseParticleId(StringArgumentType.getString(context, "particle"));
        if (!isSupportedParticle(particleId)) {
            source.sendFailure(Component.translatable("command.zovcapture.particle.invalid", particleId.toString()));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.setBaseZoneParticle(particleId);
        CaptureNetworking.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.base.setparticle.success", particleId.toString()), true);
        return 1;
    }

    private static int setCaptain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String team = StringArgumentType.getString(context, "team");
        ServerPlayer player = EntityArgument.getPlayer(context, "player");

        if (source.getServer().getScoreboard().getPlayerTeam(team) == null) {
            source.sendFailure(Component.translatable("command.zovcapture.captain.team.not_found", team));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.setTeamCaptain(team, player.getUUID());
        EconomyNetworking.syncTeam(team, source.getServer());
        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.captain.set.success",
                team,
                player.getGameProfile().getName()
        ), true);
        return 1;
    }

    private static int clearCaptain(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String team = StringArgumentType.getString(context, "team");

        if (source.getServer().getScoreboard().getPlayerTeam(team) == null) {
            source.sendFailure(Component.translatable("command.zovcapture.captain.team.not_found", team));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.clearTeamCaptain(team);
        EconomyNetworking.syncTeam(team, source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.captain.clear.success", team), true);
        return 1;
    }

    private static int setKillReward(CommandContext<CommandSourceStack> context, boolean personal) {
        CommandSourceStack source = context.getSource();
        int amount = IntegerArgumentType.getInteger(context, "amount");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        if (personal) {
            data.economyRules().setKillRewardPersonal(amount);
            source.sendSuccess(() -> Component.translatable("command.zovcapture.economy.set.kill.personal", amount), true);
        } else {
            data.economyRules().setKillRewardTeam(amount);
            source.sendSuccess(() -> Component.translatable("command.zovcapture.economy.set.kill.team", amount), true);
        }
        data.setDirty();
        return 1;
    }

    private static int setHoldReward(CommandContext<CommandSourceStack> context, boolean personal) {
        CommandSourceStack source = context.getSource();
        int amount = IntegerArgumentType.getInteger(context, "amount");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        if (personal) {
            data.economyRules().setHoldPointPersonalPerSecond(amount);
            source.sendSuccess(() -> Component.translatable("command.zovcapture.economy.set.hold.personal", amount), true);
        } else {
            data.economyRules().setHoldPointTeamPerSecond(amount);
            source.sendSuccess(() -> Component.translatable("command.zovcapture.economy.set.hold.team", amount), true);
        }
        data.setDirty();
        return 1;
    }

    private static int setHoldPersonalMode(CommandContext<CommandSourceStack> context, EconomyRules.HoldPersonalMode mode) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.economyRules().setHoldPersonalMode(mode);
        data.setDirty();
        source.sendSuccess(() -> Component.translatable("command.zovcapture.economy.set.hold.mode", mode.name().toLowerCase()), true);
        return 1;
    }

    private static int setHoldInterval(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.economyRules().setHoldRewardIntervalSeconds(seconds);
        EconomyManager.resetHoldRewardCounter();
        data.setDirty();
        source.sendSuccess(() -> Component.translatable("command.zovcapture.economy.set.hold.interval", seconds), true);
        return 1;
    }

    private static int givePersonalMoney(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        data.addPersonalMoney(player.getUUID(), amount);
        EconomyNetworking.syncPlayer(player);
        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.economy.give.personal",
                amount,
                player.getGameProfile().getName()
        ), true);
        return 1;
    }

    private static int giveTeamMoney(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String team = StringArgumentType.getString(context, "team");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (source.getServer().getScoreboard().getPlayerTeam(team) == null) {
            source.sendFailure(Component.translatable("command.zovcapture.economy.team.not_found", team));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        data.addTeamMoney(team, amount);
        EconomyNetworking.syncTeam(team, source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.economy.give.team", amount, team), true);
        return 1;
    }

    private static int takePersonalMoney(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        EconomyManager.takePersonal(data, player.getUUID(), amount);
        EconomyNetworking.syncPlayer(player);
        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.economy.take.personal",
                amount,
                player.getGameProfile().getName()
        ), true);
        data.setDirty();
        return 1;
    }

    private static int takeTeamMoney(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String team = StringArgumentType.getString(context, "team");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (source.getServer().getScoreboard().getPlayerTeam(team) == null) {
            source.sendFailure(Component.translatable("command.zovcapture.economy.team.not_found", team));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        EconomyManager.takeTeam(data, team, amount);
        EconomyNetworking.syncTeam(team, source.getServer());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.economy.take.team", amount, team), true);
        data.setDirty();
        return 1;
    }

    private static int showMoney(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        int personal = data.getPersonalMoney(player.getUUID());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.money.personal", personal), false);

        Team team = player.getTeam();
        if (team == null) {
            source.sendSuccess(() -> Component.translatable("command.zovcapture.money.no_team"), false);
            return 1;
        }

        int teamMoney = data.getTeamMoney(team.getName());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.money.team", team.getName(), teamMoney), false);
        return 1;
    }

    private static int giveTool(CommandContext<CommandSourceStack> context, @Nullable ServerPlayer target, int count)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = target != null ? target : source.getPlayerOrException();

        ItemStack stack = new ItemStack(ZovCaptureItems.SIEGE_BREAKER.get(), count);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }

        source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.tool.give.success",
                count,
                player.getGameProfile().getName()
        ), true);
        return 1;
    }

    private static int openMenu(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        PacketDistributor.sendToPlayer(player, new OpenMenuPayload());
        source.sendSuccess(() -> Component.translatable("command.zovcapture.menu.opened"), true);
        return 1;
    }

    static boolean isSupportedParticle(ResourceLocation particleId) {
        return ParticleUtils.isSupportedParticle(particleId);
    }

    private static boolean isSimpleParticle(ResourceLocation particleId) {
        return isSupportedParticle(particleId);
    }

    @Nullable
    private static ResourceLocation parseItemId(String raw) {
        if (raw.contains(":")) {
            return ResourceLocation.tryParse(raw);
        }
        return ResourceLocation.fromNamespaceAndPath("minecraft", raw);
    }

    private static boolean isValidItem(ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceKey.create(Registries.ITEM, itemId)).orElse(Items.AIR);
        return item != Items.AIR;
    }

    private static int setStat(CommandContext<CommandSourceStack> context, @Nullable String team) {
        CommandSourceStack source = context.getSource();
        String fieldRaw = StringArgumentType.getString(context, "field");
        double value = DoubleArgumentType.getDouble(context, "value");

        MatchStatRules.StatField field = MatchStatRules.parseField(fieldRaw).orElse(null);
        if (field == null) {
            source.sendFailure(Component.translatable("command.zovcapture.stats.field.invalid", fieldRaw));
            return 0;
        }

        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        MatchStatManager.setRule(data, team, field, value);

        if (data.gameActive() && !data.matchFinished()) {
            if (team == null) {
                MatchStatManager.applyAll(source.getServer(), data);
            } else {
                for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                    if (player.getTeam() != null && team.equals(player.getTeam().getName())) {
                        MatchStatManager.revertPlayer(player);
                        MatchStatManager.applyToPlayer(player, data);
                    }
                }
            }
        }

        source.sendSuccess(() -> team == null
                ? Component.translatable("command.zovcapture.stats.set.global", fieldRaw, value)
                : Component.translatable("command.zovcapture.stats.set.team", team, fieldRaw, value), true);
        return 1;
    }

    private static int clearStats(CommandContext<CommandSourceStack> context, @Nullable String team) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        if (team == null) {
            MatchStatManager.clearRules(data);
            MatchStatManager.revertAll(source.getServer());
            source.sendSuccess(() -> Component.translatable("command.zovcapture.stats.clear.all"), true);
        } else {
            data.clearTeamStatRules(team);
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                if (player.getTeam() != null && team.equals(player.getTeam().getName())) {
                    MatchStatManager.revertPlayer(player);
                    if (data.gameActive() && !data.matchFinished()) {
                        MatchStatManager.applyToPlayer(player, data);
                    }
                }
            }
            source.sendSuccess(() -> Component.translatable("command.zovcapture.stats.clear.team", team), true);
        }
        return 1;
    }

    private static int applyStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());
        MatchStatManager.revertAll(source.getServer());
        MatchStatManager.applyAll(source.getServer(), data);
        source.sendSuccess(() -> Component.translatable("command.zovcapture.stats.apply.success"), true);
        return 1;
    }

    private static int listStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CaptureGameData data = CaptureGameManager.getData(source.getLevel());

        appendStatRules(source, "command.zovcapture.stats.list.global", data.globalStatRules());
        if (data.teamStatRules().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zovcapture.stats.list.teams.empty"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("command.zovcapture.stats.list.teams.header"), false);
        for (Map.Entry<String, MatchStatRules> entry : data.teamStatRules().entrySet()) {
            String team = entry.getKey();
            source.sendSuccess(() -> Component.literal("[" + team + "]"), false);
            appendStatRules(source, null, entry.getValue());
        }
        return data.teamStatRules().size() + 1;
    }

    private static void appendStatRules(CommandSourceStack source, @Nullable String headerKey, MatchStatRules rules) {
        if (headerKey != null) {
            source.sendSuccess(() -> Component.translatable(headerKey), false);
        }
        if (rules.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.zovcapture.stats.list.empty"), false);
            return;
        }
        rules.maxHealth().ifPresent(value -> source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.stats.list.entry", "maxhealth", value), false));
        rules.movementSpeed().ifPresent(value -> source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.stats.list.entry", "speed", value), false));
        rules.attackDamage().ifPresent(value -> source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.stats.list.entry", "damage", value), false));
        rules.armor().ifPresent(value -> source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.stats.list.entry", "armor", value), false));
        rules.attackSpeed().ifPresent(value -> source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.stats.list.entry", "attackspeed", value), false));
        rules.knockbackResistance().ifPresent(value -> source.sendSuccess(() -> Component.translatable(
                "command.zovcapture.stats.list.entry", "knockback", value), false));
    }
}
