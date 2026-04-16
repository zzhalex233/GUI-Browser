package com.zzhalex233.guibrowser.client.popup;

public final class GuiRestoreFailedToast {

    private static final long DURATION_MS = 3000;

    private static String message;
    private static long showTime;

    private GuiRestoreFailedToast() {
    }

    public static void show(String text) {
        message = text;
        showTime = System.currentTimeMillis();
    }

    public static boolean isActive() {
        return message != null && System.currentTimeMillis() - showTime < DURATION_MS;
    }

    public static String getMessage() {
        return message;
    }

    public static float getAlpha() {
        long elapsed = System.currentTimeMillis() - showTime;
        if (elapsed > DURATION_MS) return 0f;
        if (elapsed > DURATION_MS - 500) {
            return (DURATION_MS - elapsed) / 500f;
        }
        return 1f;
    }
}
