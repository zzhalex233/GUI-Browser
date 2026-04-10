package com.zzhalex233.guibrowser.client.browser;

import com.zzhalex233.guibrowser.client.gui.BrowserChromeTexture;

public final class BrowserWindowState {
    private static final int DEFAULT_X = 40;
    private static final int DEFAULT_Y = 30;
    private static final int DEFAULT_WIDTH = BrowserChromeTexture.FRAME_WIDTH;
    private static final int DEFAULT_HEIGHT = BrowserChromeTexture.FRAME_HEIGHT;

    private int x;
    private int y;
    private final int width;
    private final int height;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public BrowserWindowState(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static BrowserWindowState defaultWindow() {
        return new BrowserWindowState(DEFAULT_X, DEFAULT_Y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public void beginDrag(int mouseX, int mouseY, int dragOffsetX, int dragOffsetY) {
        this.dragging = true;
        this.dragOffsetX = dragOffsetX;
        this.dragOffsetY = dragOffsetY;
    }

    public void dragTo(int mouseX, int mouseY, int viewportWidth, int viewportHeight) {
        if (!dragging) {
            return;
        }
        this.x = mouseX - dragOffsetX;
        this.y = mouseY - dragOffsetY;
        clampToViewport(viewportWidth, viewportHeight);
    }

    public void clampToViewport(int viewportWidth, int viewportHeight) {
        int maxX = Math.max(0, viewportWidth - width);
        int maxY = Math.max(0, viewportHeight - height);
        this.x = clamp(x, 0, maxX);
        this.y = clamp(y, 0, maxY);
    }

    public void endDrag() {
        this.dragging = false;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isDragging() {
        return dragging;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}