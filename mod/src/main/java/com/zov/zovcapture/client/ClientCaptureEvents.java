package com.zov.zovcapture.client;

import com.zov.zovcapture.ZovCaptureMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ZovCaptureMod.MOD_ID, value = Dist.CLIENT)
public final class ClientCaptureEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ZoneParticleRenderer.tick();
    }
}
