package com.zzhalex233.guibrowser.client.chrome;

import net.minecraft.client.gui.GuiScreen;

public final class GuiLayoutPolicy {

    private GuiLayoutPolicy() {
    }

    public static GuiLayoutState forScreen(GuiScreen screen, int screenWidth, int screenHeight) {
        return new GuiLayoutState(GuiLayoutState.LayoutMode.OVERLAY, 0);
    }
}
