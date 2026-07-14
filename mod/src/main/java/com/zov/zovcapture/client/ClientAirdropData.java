package com.zov.zovcapture.client;

import com.zov.zovcapture.network.AirdropAdminSyncPayload;

import java.util.Collections;
import java.util.List;

public final class ClientAirdropData {
    private static boolean enabled;
    private static int intervalSeconds;
    private static int captureSeconds;
    private static String particle = "minecraft:dust";
    private static List<AirdropAdminSyncPayload.AirdropSpawnPointSync> spawnPoints = Collections.emptyList();
    private static List<AirdropAdminSyncPayload.AirdropLootSync> loot = Collections.emptyList();

    private ClientAirdropData() {
    }

    public static void update(AirdropAdminSyncPayload payload) {
        enabled = payload.enabled();
        intervalSeconds = payload.intervalSeconds();
        captureSeconds = payload.captureSeconds();
        particle = payload.particle().toString();
        spawnPoints = List.copyOf(payload.spawnPoints());
        loot = List.copyOf(payload.loot());
    }

    public static boolean enabled() {
        return enabled;
    }

    public static int intervalSeconds() {
        return intervalSeconds;
    }

    public static int captureSeconds() {
        return captureSeconds;
    }

    public static String particle() {
        return particle;
    }

    public static List<AirdropAdminSyncPayload.AirdropSpawnPointSync> spawnPoints() {
        return spawnPoints;
    }

    public static List<AirdropAdminSyncPayload.AirdropLootSync> loot() {
        return loot;
    }
}
