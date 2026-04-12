package com.zzhalex233.guibrowser.client.chrome;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiLayoutPolicyTest {

    @Test
    void regularGuiScreensDefaultToOverlayMode() {
        GuiLayoutState state = GuiLayoutPolicy.forScreen(new GuiChat(), 320, 240);
        assertEquals(GuiLayoutState.LayoutMode.OVERLAY, state.getMode());
    }

    @Test
    void guiContainersUsePushDownByDefault() {
        GuiContainer container = new GuiContainer();
        GuiLayoutState state = GuiLayoutPolicy.forScreen(container, 320, 240);
        assertEquals(GuiLayoutState.LayoutMode.PUSH_DOWN, state.getMode());
    }

    @Test
    void pushDownOffsetMatchesTopBarHeight() {
        GuiContainer container = new GuiContainer();
        GuiLayoutState state = GuiLayoutPolicy.forScreen(container, 320, 240);
        assertEquals(GuiChromeLayout.TOP_BAR_HEIGHT, state.getVerticalOffset());
    }

    @Test
    void overlayModeHasZeroOffset() {
        GuiLayoutState state = GuiLayoutPolicy.forScreen(new GuiChat(), 320, 240);
        assertEquals(0, state.getVerticalOffset());
    }

    @Test
    void anonymousGuiScreenSubclassUsesOverlay() {
        GuiScreen custom = new GuiScreen() {
        };
        GuiLayoutState state = GuiLayoutPolicy.forScreen(custom, 320, 240);
        assertEquals(GuiLayoutState.LayoutMode.OVERLAY, state.getMode());
    }

    @Test
    void anonymousGuiContainerSubclassUsesPushDown() {
        GuiContainer custom = new GuiContainer() {
        };
        GuiLayoutState state = GuiLayoutPolicy.forScreen(custom, 320, 240);
        assertEquals(GuiLayoutState.LayoutMode.PUSH_DOWN, state.getMode());
    }
}
