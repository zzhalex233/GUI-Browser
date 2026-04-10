package com.zzhalex233.guibrowser.client.browser;

public final class BrowserCaptureRequest {
    public static final int DEFAULT_TICKS_TO_LIVE = 10;

    private final int ticksRemaining;
    private final boolean triggeredByHotkey;

    private BrowserCaptureRequest(int ticksRemaining, boolean triggeredByHotkey) {
        this.ticksRemaining = ticksRemaining;
        this.triggeredByHotkey = triggeredByHotkey;
    }

    public static BrowserCaptureRequest hotkeyRequest() {
        return new BrowserCaptureRequest(DEFAULT_TICKS_TO_LIVE, true);
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public boolean isTriggeredByHotkey() {
        return triggeredByHotkey;
    }

    public BrowserCaptureRequest tick() {
        if (ticksRemaining <= 1) {
            return null;
        }
        return new BrowserCaptureRequest(ticksRemaining - 1, triggeredByHotkey);
    }
}