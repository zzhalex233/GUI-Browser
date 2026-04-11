package com.zzhalex233.guibrowser.client.session;

import java.util.Objects;

public final class GuiSession {
    private final GuiSessionId id;
    private final Object screen;
    private final String title;
    private final long createdAt;
    private long lastActivatedAt;
    private boolean foreground;
    private boolean hidden;

    GuiSession(GuiSessionId id, Object screen, String title, long createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.screen = Objects.requireNonNull(screen, "screen");
        this.title = Objects.requireNonNull(title, "title");
        this.createdAt = createdAt;
        this.lastActivatedAt = createdAt;
        this.foreground = true;
        this.hidden = false;
    }

    public GuiSessionId getId() {
        return id;
    }

    public Object getScreen() {
        return screen;
    }

    public String getTitle() {
        return title;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastActivatedAt() {
        return lastActivatedAt;
    }

    public boolean isForeground() {
        return foreground;
    }

    public boolean isHidden() {
        return hidden;
    }

    void markForeground(long now) {
        this.foreground = true;
        this.hidden = false;
        this.lastActivatedAt = now;
    }

    void markHidden() {
        this.foreground = false;
        this.hidden = true;
    }

    void clearForeground() {
        this.foreground = false;
    }
}
