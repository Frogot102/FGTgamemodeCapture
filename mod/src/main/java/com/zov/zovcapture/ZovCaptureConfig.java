package com.zov.zovcapture;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ZovCaptureConfig {
    private ZovCaptureConfig() {
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue POINTS_TO_WIN;
    public static final ModConfigSpec.IntValue SCORE_PER_CAPTURED_POINT;
    public static final ModConfigSpec.DoubleValue CAPTURE_SPEED;
    public static final ModConfigSpec.DoubleValue DECAY_SPEED;
    public static final ModConfigSpec.IntValue TICK_INTERVAL;
    public static final ModConfigSpec.BooleanValue SHOW_PARTICLES;
    public static final ModConfigSpec.IntValue PARTICLE_DENSITY;
    public static final ModConfigSpec.IntValue MAX_BASE_ZONE_RADIUS;
    public static final ModConfigSpec.IntValue KILL_REWARD_PERSONAL;
    public static final ModConfigSpec.IntValue KILL_REWARD_TEAM;
    public static final ModConfigSpec.IntValue HOLD_REWARD_PERSONAL;
    public static final ModConfigSpec.IntValue HOLD_REWARD_TEAM;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("game");
        POINTS_TO_WIN = builder
                .comment("Team score required to win the match.")
                .defineInRange("pointsToWin", 100, 1, 100000);
        SCORE_PER_CAPTURED_POINT = builder
                .comment("Victory points awarded per second for each fully captured point held by a team.")
                .defineInRange("scorePerCapturedPointPerSecond", 5, 0, 1000);
        builder.pop();

        builder.push("economy");
        KILL_REWARD_PERSONAL = builder
                .comment("Personal money for killing an enemy player.")
                .defineInRange("killRewardPersonal", 25, 0, 100000);
        KILL_REWARD_TEAM = builder
                .comment("Team money for killing an enemy player.")
                .defineInRange("killRewardTeam", 10, 0, 100000);
        HOLD_REWARD_PERSONAL = builder
                .comment("Personal money per second for each held capture point.")
                .defineInRange("holdRewardPersonalPerSecond", 1, 0, 100000);
        HOLD_REWARD_TEAM = builder
                .comment("Team money per second for each held capture point.")
                .defineInRange("holdRewardTeamPerSecond", 3, 0, 100000);
        builder.pop();

        builder.push("capture");
        CAPTURE_SPEED = builder
                .comment("Capture progress per tick interval per dominating player (0.0 - 1.0).")
                .defineInRange("captureSpeed", 0.02, 0.001, 1.0);
        DECAY_SPEED = builder
                .comment("Progress lost per tick interval when zone is neutralized or contested.")
                .defineInRange("decaySpeed", 0.015, 0.0, 1.0);
        TICK_INTERVAL = builder
                .comment("How often capture logic runs, in server ticks.")
                .defineInRange("tickInterval", 10, 1, 200);
        builder.pop();

        builder.push("client");
        SHOW_PARTICLES = builder
                .comment("Render zone boundary particles on clients.")
                .define("showParticles", true);
        PARTICLE_DENSITY = builder
                .comment("Number of particle points around each zone circle.")
                .defineInRange("particleDensity", 80, 16, 256);
        builder.pop();

        builder.push("base");
        MAX_BASE_ZONE_RADIUS = builder
                .comment("Maximum radius for /zovcapture base create.")
                .defineInRange("maxBaseZoneRadius", 80, 3, 256);
        builder.pop();

        SPEC = builder.build();
    }
}
