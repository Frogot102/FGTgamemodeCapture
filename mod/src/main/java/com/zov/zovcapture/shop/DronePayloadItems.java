package com.zov.zovcapture.shop;

import java.util.Locale;
import java.util.Set;

/**
 * Items that can be mounted on SBW drones as payloads (see superbwarfare/sbw/drone_attachments).
 */
public final class DronePayloadItems {
    private static final Set<String> PAYLOAD_SHORT_IDS = Set.of(
            "rgo_grenade",
            "mortar_shell",
            "grenade_40mm",
            "c4_bomb",
            "blu_43_mine",
            "tm_62",
            "rpg_rocket_standard",
            "rpg_rocket_tbg",
            "medical_kit"
    );

    private DronePayloadItems() {
    }

    public static boolean isDronePayload(String itemOrEntityId) {
        if (itemOrEntityId == null || itemOrEntityId.isBlank()) {
            return false;
        }
        String shortId = itemOrEntityId.contains(":")
                ? itemOrEntityId.substring(itemOrEntityId.indexOf(':') + 1)
                : itemOrEntityId;
        return PAYLOAD_SHORT_IDS.contains(shortId.toLowerCase(Locale.ROOT));
    }
}
