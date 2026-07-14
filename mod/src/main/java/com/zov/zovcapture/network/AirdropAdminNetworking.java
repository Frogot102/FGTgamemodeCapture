package com.zov.zovcapture.network;

import com.zov.zovcapture.airdrop.AirdropLootEntry;
import com.zov.zovcapture.airdrop.AirdropSpawnPoint;
import com.zov.zovcapture.client.ClientAirdropData;
import com.zov.zovcapture.game.CaptureGameData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class AirdropAdminNetworking {
    private AirdropAdminNetworking() {
    }

    public static void handleClientSync(AirdropAdminSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientAirdropData.update(payload));
    }

    public static void syncPlayer(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            return;
        }
        player.connection.send(createPayload(player));
    }

    private static AirdropAdminSyncPayload createPayload(ServerPlayer player) {
        CaptureGameData data = CaptureGameData.get(player.server.overworld());

        List<AirdropAdminSyncPayload.AirdropSpawnPointSync> points = new ArrayList<>();
        for (AirdropSpawnPoint point : data.airdropSpawnPoints().values()) {
            points.add(new AirdropAdminSyncPayload.AirdropSpawnPointSync(
                    point.id(),
                    point.displayName(),
                    point.center(),
                    point.radius(),
                    point.dimension().location()
            ));
        }

        List<AirdropAdminSyncPayload.AirdropLootSync> loot = new ArrayList<>();
        for (AirdropLootEntry entry : data.airdropLoot().values()) {
            loot.add(new AirdropAdminSyncPayload.AirdropLootSync(
                    entry.id(),
                    entry.displayName(),
                    entry.weight(),
                    entry.type().name(),
                    entry.payload(),
                    entry.count(),
                    entry.previewLines()
            ));
        }

        return new AirdropAdminSyncPayload(
                data.airdropEnabled(),
                data.airdropIntervalSeconds(),
                data.airdropCaptureSeconds(),
                data.airdropParticle(),
                points,
                loot
        );
    }
}
