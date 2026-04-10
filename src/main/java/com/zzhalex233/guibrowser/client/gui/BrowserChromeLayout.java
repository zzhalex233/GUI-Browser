package com.zzhalex233.guibrowser.client.gui;

import com.zzhalex233.guibrowser.client.browser.BrowserWindowState;

public final class BrowserChromeLayout {
    private BrowserChromeLayout() {
    }

    public static Rect frame(BrowserWindowState window) {
        return new Rect(window.getX(), window.getY(), window.getWidth(), window.getHeight());
    }

    public static Rect titleBar(BrowserWindowState window) {
        return new Rect(window.getX(), window.getY(), window.getWidth(), BrowserChromeTexture.PAGE_Y);
    }

    public static Rect tabStrip(BrowserWindowState window) {
        Rect firstTab = tab(window, 0);
        return new Rect(window.getX(), firstTab.getY(), window.getWidth(), firstTab.getHeight());
    }

    public static Rect contentArea(BrowserWindowState window) {
        return new Rect(
            window.getX() + BrowserChromeTexture.PAGE_X,
            window.getY() + BrowserChromeTexture.PAGE_Y,
            BrowserChromeTexture.PAGE_WIDTH,
            BrowserChromeTexture.PAGE_HEIGHT
        );
    }

    public static Rect tab(BrowserWindowState window, int index) {
        int clampedIndex = Math.max(0, index);
        int x = window.getX() + (clampedIndex * BrowserChromeTexture.TAB_STEP);
        int y = window.getY() + BrowserChromeTexture.TAB_BASELINE_Y - BrowserChromeTexture.TAB_HEIGHT;
        return new Rect(x, y, BrowserChromeTexture.TAB_WIDTH, BrowserChromeTexture.TAB_HEIGHT);
    }

    public static Rect closeButton(BrowserWindowState window) {
        return new Rect(
            window.getX() + BrowserChromeTexture.CLOSE_U,
            window.getY() + BrowserChromeTexture.BUTTON_V,
            BrowserChromeTexture.CLOSE_WIDTH,
            BrowserChromeTexture.BUTTON_HEIGHT
        );
    }

    public static Rect minimizeButton(BrowserWindowState window) {
        return new Rect(
            window.getX() + BrowserChromeTexture.MINIMIZE_U,
            window.getY() + BrowserChromeTexture.BUTTON_V,
            BrowserChromeTexture.MINIMIZE_WIDTH,
            BrowserChromeTexture.BUTTON_HEIGHT
        );
    }

    public static Rect dragRegion(BrowserWindowState window) {
        return new Rect(
            window.getX(),
            window.getY(),
            BrowserChromeTexture.MINIMIZE_U,
            BrowserChromeTexture.PAGE_Y
        );
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