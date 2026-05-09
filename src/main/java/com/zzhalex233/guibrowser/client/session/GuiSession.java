package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;

import java.util.Objects;

import javax.annotation.Nullable;

public final class GuiSession {
    private final GuiSessionId id;
    private GuiScreen screen;
    private final String title;
    private final long createdAt;
    private long lastActivatedAt;
    private boolean foreground;
    private boolean hidden;
    @Nullable
    private GuiSessionSource source;

    GuiSession(GuiSessionId id, GuiScreen screen, String title, long createdAt, @Nullable GuiSessionSource source) {
        this.id = Objects.requireNonNull(id, "id");
        this.screen = Objects.requireNonNull(screen, "screen");
        this.title = Objects.requireNonNull(title, "title");
        this.createdAt = createdAt;
        this.lastActivatedAt = createdAt;
        this.foreground = true;
        this.hidden = false;
        this.source = source;
    }

    public GuiSessionId getId() {
        return id;
    }

    public GuiScreen getScreen() {
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

    void clearForeground() {
        this.foreground = false;
    }

    void markHidden() {
        this.hidden = true;
        this.foreground = false;
    }

    @Nullable
    public GuiSessionSource getSource() {
        return source;
    }

    void updateScreen(GuiScreen newScreen) {
        this.screen = Objects.requireNonNull(newScreen, "newScreen");
    }

    void updateSource(@Nullable GuiSessionSource newSource) {
        this.source = newSource;
    }
}
