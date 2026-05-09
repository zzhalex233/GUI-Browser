package com.zzhalex233.guibrowser.client.popup;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

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

    public static void draw(int screenWidth, FontRenderer fontRenderer, int toastY) {
        if (!isActive()) return;
        float alpha = getAlpha();
        String msg = fontRenderer.trimStringToWidth(getMessage(), Math.max(20, screenWidth - 20));
        int textW = fontRenderer.getStringWidth(msg);
        int toastW = textW + 12;
        int toastX = (screenWidth - toastW) / 2;
        int a = (int) (alpha * 0xDD) << 24;
        Gui.drawRect(toastX, toastY, toastX + toastW, toastY + 16, a | 0x331111);
        int textAlpha = (int) (alpha * 255) << 24;
        fontRenderer.drawString(msg, toastX + 6, toastY + 4, textAlpha | 0xFF6666);
    }
}
