package com.zzhalex233.guibrowser.client.chrome;

public final class GuiLayoutState {

    public enum LayoutMode {
        OVERLAY,
        PUSH_DOWN
    }

    private final LayoutMode mode;
    private final int verticalOffset;

    GuiLayoutState(LayoutMode mode, int verticalOffset) {
        this.mode = mode;
        this.verticalOffset = verticalOffset;
    }

    public LayoutMode getMode() {
        return mode;
    }

    public int getVerticalOffset() {
        return verticalOffset;
    }
}
