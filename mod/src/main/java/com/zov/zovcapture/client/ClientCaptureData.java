package com.zov.zovcapture.client;

import com.zov.zovcapture.network.AirdropStateSync;
import com.zov.zovcapture.network.BaseZoneSync;
import com.zov.zovcapture.network.CapturePointSync;
import com.zov.zovcapture.network.PostMatchPlayerStatsSync;
import com.zov.zovcapture.network.TeamScoreSync;
import com.zov.zovcapture.network.TeamVisualSync;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientCaptureData {
    private static List<CapturePointSync> points = Collections.emptyList();
    private static List<TeamScoreSync> teamScores = Collections.emptyList();
    private static Map<String, TeamVisualSync> teamVisuals = Collections.emptyMap();
    private static List<BaseZoneSync> baseZones = Collections.emptyList();
    private static ResourceLocation neutralParticle = com.zov.zovcapture.game.TeamVisualSettings.DEFAULT_NEUTRAL_PARTICLE;
    private static ResourceLocation baseZoneParticle = com.zov.zovcapture.game.TeamVisualSettings.DEFAULT_BASE_ZONE_PARTICLE;
    private static int pointsToWin = 100;
    private static boolean gameActive = false;
    private static boolean baseZoneParticlesEnabled = false;
    private static AirdropStateSync airdropState = AirdropStateSync.EMPTY;
    private static boolean matchFinished = false;
    private static String winningTeam = "";
    private static List<PostMatchPlayerStatsSync> postMatchStats = Collections.emptyList();
    private static boolean matchPreparing = false;
    private static int countdownSeconds = 0;
    private static boolean previousMatchFinished = false;

    private ClientCaptureData() {
    }

    public static void update(
            List<CapturePointSync> newPoints,
            List<TeamScoreSync> newScores,
            List<TeamVisualSync> newTeamVisuals,
            ResourceLocation newNeutralParticle,
            int winScore,
            boolean active,
            List<BaseZoneSync> newBaseZones,
            boolean baseParticlesEnabled,
            ResourceLocation newBaseZoneParticle,
            AirdropStateSync newAirdropState,
            boolean newMatchFinished,
            String newWinningTeam,
            List<PostMatchPlayerStatsSync> newPostMatchStats,
            boolean newMatchPreparing,
            int newCountdownSeconds
    ) {
        points = List.copyOf(newPoints);
        teamScores = List.copyOf(newScores);
        Map<String, TeamVisualSync> visuals = new HashMap<>();
        for (TeamVisualSync visual : newTeamVisuals) {
            visuals.put(visual.team(), visual);
        }
        teamVisuals = Collections.unmodifiableMap(visuals);
        baseZones = List.copyOf(newBaseZones);
        neutralParticle = newNeutralParticle;
        baseZoneParticle = newBaseZoneParticle;
        pointsToWin = winScore;
        gameActive = active;
        baseZoneParticlesEnabled = baseParticlesEnabled;
        airdropState = newAirdropState;

        if (newMatchFinished && !previousMatchFinished) {
            MatchOverviewScreen.open();
        }
        previousMatchFinished = newMatchFinished;

        matchFinished = newMatchFinished;
        winningTeam = newWinningTeam != null ? newWinningTeam : "";
        postMatchStats = List.copyOf(newPostMatchStats);
        matchPreparing = newMatchPreparing;
        countdownSeconds = newCountdownSeconds;
    }

    public static List<CapturePointSync> points() {
        return points;
    }

    public static List<BaseZoneSync> baseZones() {
        return baseZones;
    }

    public static List<TeamScoreSync> teamScores() {
        return teamScores;
    }

    public static ResourceLocation neutralParticle() {
        return neutralParticle;
    }

    public static ResourceLocation baseZoneParticle() {
        return baseZoneParticle;
    }

    public static boolean baseZoneParticlesEnabled() {
        return baseZoneParticlesEnabled;
    }

    @Nullable
    public static TeamVisualSync teamVisual(String team) {
        return teamVisuals.get(team);
    }

    public static int pointsToWin() {
        return pointsToWin;
    }

    public static boolean gameActive() {
        return gameActive;
    }

    public static AirdropStateSync airdropState() {
        return airdropState;
    }

    public static boolean matchFinished() {
        return matchFinished;
    }

    public static String winningTeam() {
        return winningTeam;
    }

    public static List<PostMatchPlayerStatsSync> postMatchStats() {
        return postMatchStats;
    }

    public static boolean matchPreparing() {
        return matchPreparing;
    }

    public static int countdownSeconds() {
        return countdownSeconds;
    }
}
