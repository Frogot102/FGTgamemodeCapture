package com.zov.zovcapture.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Optional;

public final class VehicleContainerHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONTAINER_ITEM_CLASS =
            "com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem";

    @Nullable
    private static Method createInstanceMethod;

    private VehicleContainerHelper() {
    }

    public static Optional<ItemStack> createContainer(MinecraftServer server, String entityId) {
        return createContainer(server, entityId);
    }

    public static Optional<ItemStack> createContainer(MinecraftServer server, String entityId, String... fallbacks) {
        Optional<ItemStack> direct = tryEntityId(server, entityId);
        if (direct.isPresent()) {
            return direct;
        }
        if (fallbacks != null) {
            for (String fallback : fallbacks) {
                Optional<ItemStack> stack = tryEntityId(server, fallback);
                if (stack.isPresent()) {
                    return stack;
                }
            }
        }
        LOGGER.warn("Vehicle container could not be created for entity {}", entityId);
        return Optional.empty();
    }

    private static Optional<ItemStack> tryEntityId(MinecraftServer server, String entityId) {
        ResourceLocation location = ResourceLocation.parse(entityId);
        EntityType<?> entityType = server.registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .getOptional(ResourceKey.create(Registries.ENTITY_TYPE, location))
                .orElse(null);
        if (entityType == null) {
            entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(location).orElse(null);
        }
        if (entityType == null) {
            return Optional.empty();
        }
        return invokeCreateInstance(entityType);
    }

    private static Optional<ItemStack> invokeCreateInstance(EntityType<?> entityType) {
        Method method = resolveCreateInstanceMethod();
        if (method == null) {
            return Optional.empty();
        }
        try {
            Object result = method.invoke(null, entityType);
            if (result instanceof ItemStack stack && !stack.isEmpty()) {
                return Optional.of(stack.copy());
            }
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Failed to create superbwarfare vehicle container for {}", BuiltInRegistries.ENTITY_TYPE.getKey(entityType), exception);
        }
        return Optional.empty();
    }

    @Nullable
    private static Method resolveCreateInstanceMethod() {
        if (createInstanceMethod != null) {
            return createInstanceMethod;
        }
        try {
            Class<?> containerClass = Class.forName(CONTAINER_ITEM_CLASS);
            createInstanceMethod = containerClass.getMethod("createInstance", EntityType.class);
            createInstanceMethod.setAccessible(true);
            return createInstanceMethod;
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            LOGGER.warn("Superb Warfare container API is unavailable; vehicle containers cannot be sold");
            return null;
        }
    }
}
