package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;

import java.util.Set;

public final class GuiTrackingPolicy {

    public enum TrackingDecision {
        TRACK_AS_TAB,
        EXCLUDE
    }

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
        "net.minecraft.client.multiplayer.GuiConnecting",
        "net.minecraft.client.gui.inventory.GuiContainerCreative"
    );

    private static final Set<String> TRINKET_PACKAGE_PREFIXES = Set.of(
        "c4.curios.",
        "baubles.",
        "vazkii.botania.client.gui.bag."
    );

    private GuiTrackingPolicy() {
    }

    public static TrackingDecision decide(GuiScreen screen, boolean hasSource) {
        // 1. null -> EXCLUDE
        if (screen == null) {
            return TrackingDecision.EXCLUDE;
        }

        String className = screen.getClass().getName();

        // 2. Hard-excluded classes -> EXCLUDE
        if (EXCLUDED_SCREEN_CLASSES.contains(className)) {
            return TrackingDecision.EXCLUDE;
        }

        // 3. GuiInventory or subclass -> EXCLUDE
        if (screen instanceof GuiInventory) {
            return TrackingDecision.EXCLUDE;
        }

        // 4. Trinket mod packages -> EXCLUDE
        for (String prefix : TRINKET_PACKAGE_PREFIXES) {
            if (className.startsWith(prefix)) {
                return TrackingDecision.EXCLUDE;
            }
        }

        // 5. Mod-own popups -> EXCLUDE
        if (className.startsWith("com.zzhalex233.guibrowser.client.popup.")) {
            return TrackingDecision.EXCLUDE;
        }

        // 6. NOT instanceof GuiContainer -> EXCLUDE
        if (!(screen instanceof GuiContainer)) {
            return TrackingDecision.EXCLUDE;
        }

        // 7. GuiContainer AND hasSource -> TRACK_AS_TAB
        if (hasSource) {
            return TrackingDecision.TRACK_AS_TAB;
        }

        // 8. GuiContainer AND !hasSource -> EXCLUDE (safety fallback)
        return TrackingDecision.EXCLUDE;
    }

    /**
     * @deprecated Use {@link #decide(GuiScreen, boolean)} instead.
     */
    @Deprecated
    public static boolean shouldTrack(GuiScreen screen) {
        return decide(screen, true) == TrackingDecision.TRACK_AS_TAB;
    }
}
