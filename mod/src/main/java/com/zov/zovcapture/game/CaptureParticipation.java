package com.zov.zovcapture.game;



import net.minecraft.core.component.DataComponents;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.component.CustomData;



import java.util.Locale;



public final class CaptureParticipation {

    private static final ResourceLocation MONITOR_ID =

            ResourceLocation.fromNamespaceAndPath("superbwarfare", "monitor");



    private CaptureParticipation() {

    }



    public static boolean canParticipateInCapture(Player player) {

        if (!(player instanceof ServerPlayer serverPlayer)) {

            return false;

        }

        return canParticipateInCapture(serverPlayer);

    }



    public static boolean canParticipateInCapture(ServerPlayer player) {

        if (!player.isAlive() || player.isSpectator()) {

            return false;

        }



        if (isControllingDroneViaMonitor(player)) {

            return false;

        }



        Entity camera = player.getCamera();

        if (camera != null && camera != player) {

            return false;

        }



        for (Entity ridden = player.getVehicle(); ridden != null; ridden = ridden.getVehicle()) {

            if (isDroneEntity(ridden)) {

                return false;

            }

        }



        return true;

    }



    /**

     * SBW stores active drone view on the monitor item ({@code Using=true} in custom data).

     */

    private static boolean isControllingDroneViaMonitor(ServerPlayer player) {

        for (ItemStack stack : player.getInventory().items) {

            if (isMonitorInDroneView(stack)) {

                return true;

            }

        }

        return isMonitorInDroneView(player.getOffhandItem());

    }



    private static boolean isMonitorInDroneView(ItemStack stack) {

        if (stack.isEmpty()) {

            return false;

        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (!MONITOR_ID.equals(itemId)) {

            return false;

        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData == null) {

            return false;

        }

        CompoundTag tag = customData.copyTag();

        return tag.getBoolean("Using");

    }



    private static boolean isDroneEntity(Entity entity) {

        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        if (key == null || !"superbwarfare".equals(key.getNamespace())) {

            return false;

        }

        return key.getPath().toLowerCase(Locale.ROOT).contains("drone");

    }

}


