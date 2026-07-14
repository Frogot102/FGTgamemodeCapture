package com.zov.zovcapture.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.zov.zovcapture.ZovCaptureMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ZovCaptureKeys {
    public static final String CATEGORY = "key.categories." + ZovCaptureMod.MOD_ID;

    public static final KeyMapping OPEN_SHOP = new KeyMapping(
            "key.zovcapture.open_shop",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    public static final KeyMapping OPEN_TEAMS = new KeyMapping(
            "key.zovcapture.open_teams",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    public static final KeyMapping OPEN_MATCH = new KeyMapping(
            "key.zovcapture.open_match",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    public static final KeyMapping OPEN_CLASS = new KeyMapping(
            "key.zovcapture.open_class",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    private ZovCaptureKeys() {
    }
}
