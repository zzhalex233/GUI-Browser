package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;

import java.util.Set;

public final class GuiTrackingPolicy {
    private static final Set<String> EXCLUDED_SCREEN_CLASSES = Set.of(
        "net.minecraft.client.gui.GuiMainMenu",
        "net.minecraft.client.gui.GuiMultiplayer",
        "net.minecraft.client.gui.GuiWorldSelection",
        "net.minecraft.client.gui.GuiDisconnected",
        "net.minecraft.client.gui.GuiDownloadTerrain",
        "net.minecraft.client.gui.GuiErrorScreen",
        "net.minecraft.client.gui.GuiGameOver",
        "net.minecraft.client.gui.GuiMemoryErrorScreen",
        "net.minecraft.client.gui.GuiScreenWorking",
        "net.minecraft.client.multiplayer.GuiConnecting"
    );

    private GuiTrackingPolicy() {
    }

    public static boolean shouldTrack(GuiScreen screen) {
        if (screen == null) {
            return false;
        }

        String className = screen.getClass().getName();
        if (EXCLUDED_SCREEN_CLASSES.contains(className)) {
            return false;
        }

        return !className.startsWith("com.zzhalex233.guibrowser.client.popup.");
    }
}
