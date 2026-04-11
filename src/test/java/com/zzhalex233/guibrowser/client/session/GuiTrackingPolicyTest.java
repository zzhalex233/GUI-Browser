package com.zzhalex233.guibrowser.client.session;

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

    private static final class GuiMainMenu {
    }

    private static final class GuiConnecting {
    }

    private static final class GuiChat {
    }
}