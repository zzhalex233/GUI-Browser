package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;

public final class GuiSessionTitleResolver {
    private GuiSessionTitleResolver() {
    }

    public static String resolve(GuiScreen screen, String explicitTitle) {
        if (explicitTitle != null) {
            String trimmedTitle = explicitTitle.trim();
            if (!trimmedTitle.isEmpty()) {
                return trimmedTitle;
            }
        }
        if (screen != null) {
            String simpleName = screen.getClass().getSimpleName();
            if (simpleName != null && !simpleName.trim().isEmpty()) {
                return simpleName.trim();
            }
        }
        return "GUI";
    }
}
