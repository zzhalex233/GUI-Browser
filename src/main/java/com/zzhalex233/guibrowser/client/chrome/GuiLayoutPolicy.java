package com.zzhalex233.guibrowser.client.chrome;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

public final class GuiLayoutPolicy {

    private GuiLayoutPolicy() {
    }

    public static GuiLayoutState forScreen(GuiScreen screen, int screenWidth, int screenHeight) {
        if (screen instanceof GuiContainer) {
            return new GuiLayoutState(
                    GuiLayoutState.LayoutMode.PUSH_DOWN,
                    GuiChromeLayout.TOP_BAR_HEIGHT
            );
        }
        return new GuiLayoutState(GuiLayoutState.LayoutMode.OVERLAY, 0);
    }
}
