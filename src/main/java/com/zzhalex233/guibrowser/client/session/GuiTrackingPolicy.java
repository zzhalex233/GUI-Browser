package com.zzhalex233.guibrowser.client.session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class GuiTrackingPolicy {
    private static final Set<String> EXCLUDED_SIMPLE_NAMES = new HashSet<>(Arrays.asList(
        "GuiMainMenu",
        "GuiMultiplayer",
        "GuiWorldSelection",
        "GuiConnecting",
        "GuiDownloadTerrain",
        "GuiDisconnected",
        "GuiGameOver",
        "GuiMemoryErrorScreen",
        "GuiErrorScreen"
    ));

    private GuiTrackingPolicy() {
    }

    public static boolean shouldTrack(Object screen) {
        if (screen == null) {
            return false;
        }
        return !EXCLUDED_SIMPLE_NAMES.contains(screen.getClass().getSimpleName());
    }
}