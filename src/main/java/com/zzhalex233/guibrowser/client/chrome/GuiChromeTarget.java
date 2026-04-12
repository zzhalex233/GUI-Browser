package com.zzhalex233.guibrowser.client.chrome;

public final class GuiChromeTarget {

    public enum Type {
        NONE,
        TAB,
        TAB_CLOSE,
        BOOKMARK_BUTTON,
        HISTORY_BUTTON
    }

    private static final GuiChromeTarget NONE_TARGET = new GuiChromeTarget(Type.NONE, -1);
    private static final GuiChromeTarget BOOKMARK_TARGET = new GuiChromeTarget(Type.BOOKMARK_BUTTON, -1);
    private static final GuiChromeTarget HISTORY_TARGET = new GuiChromeTarget(Type.HISTORY_BUTTON, -1);

    private final Type type;
    private final int tabIndex;

    private GuiChromeTarget(Type type, int tabIndex) {
        this.type = type;
        this.tabIndex = tabIndex;
    }

    public static GuiChromeTarget none() {
        return NONE_TARGET;
    }

    public static GuiChromeTarget tab(int index) {
        return new GuiChromeTarget(Type.TAB, index);
    }

    public static GuiChromeTarget tabClose(int index) {
        return new GuiChromeTarget(Type.TAB_CLOSE, index);
    }

    public static GuiChromeTarget bookmarkButton() {
        return BOOKMARK_TARGET;
    }

    public static GuiChromeTarget historyButton() {
        return HISTORY_TARGET;
    }

    public Type getType() {
        return type;
    }

    public int getTabIndex() {
        return tabIndex;
    }
}
