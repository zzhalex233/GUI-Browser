package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiTrackingPolicyTest {

    @Test
    void mainMenuAndConnectionScreensAreNeverTracked() {
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiMainMenu()));
        assertFalse(GuiTrackingPolicy.shouldTrack(new GuiConnecting()));
    }

    @Test
    void regularContainerAndRegularGuiScreensAreTracked() {
        assertTrue(GuiTrackingPolicy.shouldTrack(new GuiChat()));
    }
}
