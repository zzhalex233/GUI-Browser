package com.zzhalex233.guibrowser.client.gui;

import com.zzhalex233.guibrowser.client.browser.BrowserWindowState;

public final class BrowserChromeLayout {
    private static final int TITLE_BAR_HEIGHT = 20;
    private static final int TAB_STRIP_HEIGHT = 22;
    private static final int BUTTON_SIZE = 12;
    private static final int BUTTON_PADDING = 4;
    private static final int CONTENT_GAP = 6;

    private BrowserChromeLayout() {
    }

    public static Rect titleBar(BrowserWindowState window) {
        return new Rect(window.getX(), window.getY(), window.getWidth(), TITLE_BAR_HEIGHT);
    }

    public static Rect tabStrip(BrowserWindowState window) {
        return new Rect(window.getX(), titleBar(window).getBottom(), window.getWidth(), TAB_STRIP_HEIGHT);
    }

    public static Rect contentArea(BrowserWindowState window) {
        int x = window.getX() + CONTENT_GAP;
        int y = tabStrip(window).getBottom() + CONTENT_GAP;
        int width = window.getWidth() - (CONTENT_GAP * 2);
        int height = window.getHeight() - TITLE_BAR_HEIGHT - TAB_STRIP_HEIGHT - (CONTENT_GAP * 2);
        return new Rect(x, y, width, height);
    }

    public static Rect closeButton(BrowserWindowState window) {
        Rect titleBar = titleBar(window);
        int x = titleBar.getRight() - BUTTON_PADDING - BUTTON_SIZE;
        int y = titleBar.getY() + BUTTON_PADDING;
        return new Rect(x, y, BUTTON_SIZE, BUTTON_SIZE);
    }

    public static Rect minimizeButton(BrowserWindowState window) {
        Rect closeButton = closeButton(window);
        int x = closeButton.getX() - BUTTON_PADDING - BUTTON_SIZE;
        return new Rect(x, closeButton.getY(), BUTTON_SIZE, BUTTON_SIZE);
    }

    public static Rect dragRegion(BrowserWindowState window) {
        Rect titleBar = titleBar(window);
        Rect minimizeButton = minimizeButton(window);
        int width = Math.max(0, minimizeButton.getX() - titleBar.getX() - BUTTON_PADDING);
        return new Rect(titleBar.getX(), titleBar.getY(), width, titleBar.getHeight());
    }

    public static final class Rect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        public Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
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

        public int getRight() {
            return x + width;
        }

        public int getBottom() {
            return y + height;
        }

        public boolean contains(int pointX, int pointY) {
            return pointX >= x && pointX < getRight() && pointY >= y && pointY < getBottom();
        }
    }
}
