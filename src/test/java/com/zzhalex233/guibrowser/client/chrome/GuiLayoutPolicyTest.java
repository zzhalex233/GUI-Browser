package com.zzhalex233.guibrowser.client.chrome;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiLayoutPolicyTest {

    @Test
    void regularGuiScreensUseOverlayMode() {
        GuiLayoutState state = GuiLayoutPolicy.forScreen(new GuiChat(), 320, 240);
        assertEquals(GuiLayoutState.LayoutMode.OVERLAY, state.getMode());
    }

    @Test
    void guiContainersUseOverlayMode() {
        GuiContainer container = new GuiContainer();
        GuiLayoutState state = GuiLayoutPolicy.forScreen(container, 320, 240);
        assertEquals(GuiLayoutState.LayoutMode.OVERLAY, state.getMode());
    }

    @Test
    void overlayModeHasZeroOffset() {
        GuiLayoutState state = GuiLayoutPolicy.forScreen(new GuiChat(), 320, 240);
        assertEquals(0, state.getVerticalOffset());
    }

    @Test
    void guiContainerOverlayHasZeroOffset() {
        GuiContainer container = new GuiContainer();
        GuiLayoutState state = GuiLayoutPolicy.forScreen(container, 320, 240);
        assertEquals(0, state.getVerticalOffset());
    }

    @Test
    void anonymousGuiScreenSubclassUsesOverlay() {
        GuiScreen custom = new GuiScreen() {
        };
        GuiLayoutState state = GuiLayoutPolicy.forScreen(custom, 320, 240);
        assertEquals(GuiLayoutState.LayoutMode.OVERLAY, state.getMode());
    }
}
