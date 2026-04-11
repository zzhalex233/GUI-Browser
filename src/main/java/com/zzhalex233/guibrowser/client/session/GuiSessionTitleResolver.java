package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;

public final class GuiSessionTitleResolver {
    private GuiSessionTitleResolver() {
    }

    public static String resolve(GuiScreen screen, String explicitTitle) {
        if (explicitTitle != null && !explicitTitle.trim().isEmpty()) {
            return explicitTitle.trim();
        }
        return screen == null ? "GUI" : screen.getClass().getSimpleName();
    }
}
