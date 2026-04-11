package com.zzhalex233.guibrowser.client.session;

public final class GuiSessionTitleResolver {
    private GuiSessionTitleResolver() {
    }

    public static String resolve(Object screen, String explicitTitle) {
        if (explicitTitle != null) {
            String trimmed = explicitTitle.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        if (screen == null) {
            return "GUI";
        }
        String simpleName = screen.getClass().getSimpleName();
        return simpleName == null || simpleName.trim().isEmpty() ? "GUI" : simpleName;
    }
}