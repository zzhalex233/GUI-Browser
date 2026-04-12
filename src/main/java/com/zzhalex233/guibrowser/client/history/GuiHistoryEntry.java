package com.zzhalex233.guibrowser.client.history;

public final class GuiHistoryEntry {

    public enum Action {
        OPENED,
        ACTIVATED,
        HIDDEN,
        DESTROYED,
        CLEARED_ON_UNLOAD
    }

    private final String sessionTitle;
    private final Action action;
    private final long timestamp;

    public GuiHistoryEntry(String sessionTitle, Action action, long timestamp) {
        this.sessionTitle = sessionTitle;
        this.action = action;
        this.timestamp = timestamp;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public Action getAction() {
        return action;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
