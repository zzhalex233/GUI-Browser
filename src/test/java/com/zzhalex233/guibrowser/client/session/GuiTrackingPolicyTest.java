package com.zzhalex233.guibrowser.client.session;

import com.zzhalex233.guibrowser.client.popup.FakePopupScreen;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMemoryErrorScreen;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.multiplayer.GuiConnecting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiTrackingPolicyTest {

    @Test
    void nullAndInternalPopupScreensAreNeverTracked() {
        assertFalse(GuiTrackingPolicy.shouldTrack(null));
        assertFalse(GuiTrackingPolicy.shouldTrack(new FakePopupScreen()));
    }

    @Test
    void menuProgressAndConnectionScreensAreNeverTracked() {
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiMainMenu()));
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiMultiplayer()));
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiWorldSelection()));
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiDisconnected()));
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiConnecting()));
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiDownloadTerrain()));
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiGameOver()));
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiScreenWorking()));
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiErrorScreen()));
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiMemoryErrorScreen()));
    }

    @Test
    void regularContainerAndRegularGuiScreensAreTracked() {
        assertTrue(GuiTrackingPolicy.shouldTrack(new GuiChat()));
    }
}
