package com.zov.zovcapture.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

import javax.annotation.Nullable;

public final class MatchRulesManager {
    @Nullable
    private static Boolean savedImmediateRespawn;

    private MatchRulesManager() {
    }

    public static void applyMatchRules(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            GameRules.BooleanValue rule = level.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
            if (savedImmediateRespawn == null) {
                savedImmediateRespawn = rule.get();
            }
            rule.set(true, server);
        }
    }

    public static void restoreMatchRules(MinecraftServer server) {
        if (savedImmediateRespawn == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            level.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(savedImmediateRespawn, server);
        }
        savedImmediateRespawn = null;
    }
}
