package com.zzhalex233.guibrowser.client.chrome;

public final class GuiChromeLayout {

    public static final int TOP_BAR_HEIGHT = 20;
    public static final int TAB_WIDTH = 112;
    public static final int TAB_HEIGHT = 18;
    public static final int TAB_GAP = 2;
    public static final int TAB_ICON_SIZE = 14;
    public static final int TAB_CLOSE_SIZE = 16;
    public static final int TAB_CLOSE_MARGIN = 1;
    public static final int BUTTON_SIZE = 16;
    public static final int BUTTON_GAP = 4;
    public static final int BUTTON_MARGIN_RIGHT = 4;
    public static final int TAB_LEFT_PADDING = 4;

    private final int screenWidth;
    private final int screenHeight;
    private final int scrollOffset;

    public GuiChromeLayout(int screenWidth, int screenHeight) {
        this(screenWidth, screenHeight, 0);
    }

    public GuiChromeLayout(int screenWidth, int screenHeight, int scrollOffset) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.scrollOffset = Math.max(0, scrollOffset);
    }

    public int getTopBarHeight() {
        return TOP_BAR_HEIGHT;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public Rect topBarRect() {
        return new Rect(0, 0, screenWidth, TOP_BAR_HEIGHT);
    }

    public Rect tabRect(int index) {
        int x = index * (TAB_WIDTH + TAB_GAP) - scrollOffset;
        int y = (TOP_BAR_HEIGHT - TAB_HEIGHT) / 2;
        return new Rect(x, y, TAB_WIDTH, TAB_HEIGHT);
    }

    public Rect tabCloseRect(int index) {
        Rect tab = tabRect(index);
        int closeX = tab.getRight() - TAB_CLOSE_MARGIN - TAB_CLOSE_SIZE;
        int closeY = tab.getY() + (TAB_HEIGHT - TAB_CLOSE_SIZE) / 2;
        return new Rect(closeX, closeY, TAB_CLOSE_SIZE, TAB_CLOSE_SIZE);
    }

    public Rect bookmarkButtonRect() {
        Rect history = historyButtonRect();
        int x = history.getX() - BUTTON_GAP - BUTTON_SIZE;
        int y = (TOP_BAR_HEIGHT - BUTTON_SIZE) / 2;
        return new Rect(x, y, BUTTON_SIZE, BUTTON_SIZE);
    }

    public Rect historyButtonRect() {
        int x = screenWidth - BUTTON_MARGIN_RIGHT - BUTTON_SIZE;
        int y = (TOP_BAR_HEIGHT - BUTTON_SIZE) / 2;
        return new Rect(x, y, BUTTON_SIZE, BUTTON_SIZE);
    }

    public Rect tabViewportRect() {
        int right = bookmarkButtonRect().getX() - BUTTON_GAP;
        return new Rect(0, 0, Math.max(0, right), TOP_BAR_HEIGHT);
    }

    public int maxScrollOffset(int tabCount) {
        int contentWidth = tabCount == 0 ? 0 : tabCount * TAB_WIDTH + (tabCount - 1) * TAB_GAP;
        return Math.max(0, contentWidth - tabViewportRect().getWidth());
    }

    public GuiChromeTarget hitTest(int mouseX, int mouseY, int tabCount) {
        if (!topBarRect().contains(mouseX, mouseY)) {
            return GuiChromeTarget.none();
        }

        if (historyButtonRect().contains(mouseX, mouseY)) {
            return GuiChromeTarget.historyButton();
        }

        if (bookmarkButtonRect().contains(mouseX, mouseY)) {
            return GuiChromeTarget.bookmarkButton();
        }

        if (!tabViewportRect().contains(mouseX, mouseY)) {
            return GuiChromeTarget.none();
        }

        for (int i = 0; i < tabCount; i++) {
            if (tabCloseRect(i).contains(mouseX, mouseY)) {
                return GuiChromeTarget.tabClose(i);
            }
            if (tabRect(i).contains(mouseX, mouseY)) {
                return GuiChromeTarget.tab(i);
            }
        }

        return GuiChromeTarget.none();
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

        public boolean contains(int px, int py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
}
