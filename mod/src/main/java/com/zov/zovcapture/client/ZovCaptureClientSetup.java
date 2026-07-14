package com.zov.zovcapture.client;

import com.zov.zovcapture.ZovCaptureMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = ZovCaptureMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ZovCaptureClientSetup {
    private ZovCaptureClientSetup() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ZovCaptureKeys.OPEN_SHOP);
        event.register(ZovCaptureKeys.OPEN_TEAMS);
        event.register(ZovCaptureKeys.OPEN_MATCH);
        event.register(ZovCaptureKeys.OPEN_CLASS);
    }
}
