package com.zzhalex233.guibrowser.client.browser;

public final class BrowserWindowState {
    private static final int DEFAULT_X = 40;
    private static final int DEFAULT_Y = 30;
    private static final int DEFAULT_WIDTH = 360;
    private static final int DEFAULT_HEIGHT = 240;

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
        dragTo(mouseX, mouseY, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public void dragTo(int mouseX, int mouseY, int viewportWidth, int viewportHeight) {
        if (!dragging) {
            return;
        }
        int maxX = Math.max(0, viewportWidth - width);
        int maxY = Math.max(0, viewportHeight - height);
        this.x = clamp(mouseX - dragOffsetX, 0, maxX);
        this.y = clamp(mouseY - dragOffsetY, 0, maxY);
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
