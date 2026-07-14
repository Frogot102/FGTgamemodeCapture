package com.zov.zovcapture.airdrop;

import com.zov.zovcapture.game.CaptureGameData;
import com.zov.zovcapture.game.CaptureParticipation;
import com.zov.zovcapture.game.MatchStatsTracker;
import com.zov.zovcapture.network.CaptureNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AirdropManager {
    private AirdropManager() {
    }

    public static void onServerTick(MinecraftServer server, CaptureGameData data) {
        if (!data.airdropEnabled() || !data.gameActive() || data.matchFinished()) {
            return;
        }

        boolean changed = false;
        if (data.activeAirdrop() == null) {
            if (data.airdropSpawnPoints().isEmpty() || data.airdropLoot().isEmpty()) {
                return;
            }
            long gameTime = server.overworld().getGameTime();
            if (data.nextAirdropTick() <= 0) {
                data.setNextAirdropTick(gameTime + data.airdropIntervalSeconds() * 20L);
                changed = true;
            } else if (gameTime >= data.nextAirdropTick()) {
                changed |= spawnAirdrop(server, data);
            }
        } else {
            changed |= tickActiveAirdrop(server, data);
            if (!changed && data.activeAirdrop() != null && server.getTickCount() % 10 == 0) {
                changed = true;
            }
        }

        if (changed) {
            CaptureNetworking.syncToAll(server);
        }
    }

    private static boolean spawnAirdrop(MinecraftServer server, CaptureGameData data) {
        List<AirdropSpawnPoint> points = new ArrayList<>(data.airdropSpawnPoints().values());
        if (points.isEmpty()) {
            return false;
        }

        AirdropSpawnPoint point = points.get(server.overworld().random.nextInt(points.size()));
        ServerLevel level = server.getLevel(point.dimension());
        if (level == null) {
            return false;
        }

        BlockPos cratePos = findCratePos(level, point.center());
        if (cratePos == null) {
            data.setNextAirdropTick(level.getGameTime() + 20L * 30);
            return true;
        }

        level.setBlock(cratePos, Blocks.CHEST.defaultBlockState(), 3);
        data.setActiveAirdrop(new ActiveAirdrop(
                point.id(),
                point.displayName(),
                cratePos,
                point.dimension(),
                point.radius()
        ));
        data.setNextAirdropTick(level.getGameTime() + data.airdropIntervalSeconds() * 20L);

        Component message = Component.translatable("message.zovcapture.airdrop.spawned", point.displayName());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
        return true;
    }

    @Nullable
    private static BlockPos findCratePos(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos cursor = center.mutable();
        for (int y = center.getY() + 2; y >= center.getY() - 4; y--) {
            cursor.set(center.getX(), y, center.getZ());
            BlockState below = level.getBlockState(cursor.below());
            BlockState at = level.getBlockState(cursor);
            if (!below.isAir() && below.canOcclude() && (at.isAir() || at.canBeReplaced())) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private static boolean tickActiveAirdrop(MinecraftServer server, CaptureGameData data) {
        ActiveAirdrop active = data.activeAirdrop();
        if (active == null) {
            return false;
        }

        ServerLevel level = server.getLevel(active.dimension());
        if (level == null) {
            clearAirdrop(server, data, false);
            return true;
        }

        if (!level.getBlockState(active.cratePos()).is(Blocks.CHEST)) {
            clearAirdrop(server, data, false);
            return true;
        }

        ServerPlayer capturer = findCapturer(level, active);
        if (capturer == null) {
            if (active.capturingPlayer() != null) {
                ServerPlayer previous = server.getPlayerList().getPlayer(active.capturingPlayer());
                if (previous != null) {
                    AirdropBossBar.end(previous);
                }
                active.setCapturingPlayer(null);
                active.setCaptureProgress(0.0F);
            }
            return false;
        }

        if (active.capturingPlayer() == null || !active.capturingPlayer().equals(capturer.getUUID())) {
            if (active.capturingPlayer() != null) {
                ServerPlayer previous = server.getPlayerList().getPlayer(active.capturingPlayer());
                if (previous != null) {
                    AirdropBossBar.end(previous);
                }
            }
            active.setCapturingPlayer(capturer.getUUID());
            active.setCaptureProgress(0.0F);
            AirdropBossBar.begin(capturer, active.displayName());
        }

        float step = 1.0F / Math.max(1, data.airdropCaptureSeconds() * 20);
        active.setCaptureProgress(Math.min(1.0F, active.captureProgress() + step));
        AirdropBossBar.update(active.displayName(), active.captureProgress());

        if (active.captureProgress() >= 1.0F) {
            completeCapture(server, data, capturer, active, level);
            return true;
        }
        return false;
    }

    @Nullable
    private static ServerPlayer findCapturer(ServerLevel level, ActiveAirdrop active) {
        ServerPlayer found = null;
        for (ServerPlayer player : level.players()) {
            if (!CaptureParticipation.canParticipateInCapture(player)) {
                continue;
            }
            if (!active.contains(player.blockPosition())) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = player;
        }
        return found;
    }

    private static void completeCapture(
            MinecraftServer server,
            CaptureGameData data,
            ServerPlayer player,
            ActiveAirdrop active,
            ServerLevel level
    ) {
        AirdropBossBar.end(player);
        AirdropLootRoller.rollAndGive(player, data);
        MatchStatsTracker.recordAirdrop(data, player);
        level.setBlock(active.cratePos(), Blocks.AIR.defaultBlockState(), 3);
        player.sendSystemMessage(Component.translatable("message.zovcapture.airdrop.captured", active.displayName()));
        com.zov.zovcapture.network.EconomyNetworking.syncPlayer(player);
        clearAirdrop(server, data, true);
    }

    public static void clearAirdrop(MinecraftServer server, CaptureGameData data, boolean completed) {
        ActiveAirdrop active = data.activeAirdrop();
        if (active == null) {
            return;
        }

        if (active.capturingPlayer() != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(active.capturingPlayer());
            if (player != null) {
                AirdropBossBar.end(player);
            }
        }
        AirdropBossBar.clear();

        ServerLevel level = server.getLevel(active.dimension());
        if (level != null && level.getBlockState(active.cratePos()).is(Blocks.CHEST)) {
            level.setBlock(active.cratePos(), Blocks.AIR.defaultBlockState(), 3);
        }

        data.setActiveAirdrop(null);
        if (!completed) {
            data.setNextAirdropTick(server.overworld().getGameTime() + 20L * 30);
        }
    }

    public static boolean isAirdropChest(BlockPos pos) {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        ActiveAirdrop active = CaptureGameData.get(server.overworld()).activeAirdrop();
        return active != null && active.cratePos().equals(pos);
    }

    public static int secondsUntilNext(CaptureGameData data, long gameTime) {
        if (!data.airdropEnabled()) {
            return -1;
        }
        if (data.activeAirdrop() != null) {
            return 0;
        }
        if (data.nextAirdropTick() <= 0) {
            return data.airdropIntervalSeconds();
        }
        return (int) Math.max(0, (data.nextAirdropTick() - gameTime) / 20);
    }
}
