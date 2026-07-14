package com.zov.zovcapture;

import com.zov.zovcapture.command.CaptureCommands;
import com.zov.zovcapture.game.AirdropEvents;
import com.zov.zovcapture.game.CaptureGameManager;
import com.zov.zovcapture.game.GameEvents;
import com.zov.zovcapture.game.MatchCombatRules;
import com.zov.zovcapture.game.MatchLifecycle;
import com.zov.zovcapture.game.ProtectionEvents;
import com.zov.zovcapture.item.ZovCaptureItems;
import com.zov.zovcapture.network.CaptureNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(ZovCaptureMod.MOD_ID)
public class ZovCaptureMod {
    public static final String MOD_ID = "zovcapture";

    public ZovCaptureMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ZovCaptureConfig.SPEC);

        ZovCaptureItems.ITEMS.register(modEventBus);
        modEventBus.addListener(CaptureNetworking::registerPayloads);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(ProtectionEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(ProtectionEvents::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(GameEvents::onPlayerDeath);
        NeoForge.EVENT_BUS.addListener(GameEvents::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(MatchCombatRules::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(MatchCombatRules::onLivingFall);
        NeoForge.EVENT_BUS.addListener(MatchCombatRules::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(MatchCombatRules::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(MatchCombatRules::onLivingHeal);
        NeoForge.EVENT_BUS.addListener(AirdropEvents::onChestInteract);
        NeoForge.EVENT_BUS.addListener(AirdropEvents::onBlockBreak);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CaptureCommands.register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event) {
        CaptureGameManager.onServerStarting(event.getServer());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        CaptureGameManager.onServerStopping();
    }

    private void onServerTick(ServerTickEvent.Post event) {
        CaptureGameManager.onServerTick(event.getServer());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CaptureGameManager.syncToPlayer(player);
            MatchLifecycle.onPlayerJoinMatch(player);
        }
    }
}
