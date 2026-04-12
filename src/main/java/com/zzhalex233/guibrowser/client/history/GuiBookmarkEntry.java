package com.zzhalex233.guibrowser.client.history;

public final class GuiBookmarkEntry {

    private final String sessionTitle;
    private final String screenClassName;
    private final long bookmarkedAt;

    public GuiBookmarkEntry(String sessionTitle, String screenClassName, long bookmarkedAt) {
        this.sessionTitle = sessionTitle;
        this.screenClassName = screenClassName;
        this.bookmarkedAt = bookmarkedAt;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public String getScreenClassName() {
        return screenClassName;
    }

    public long getBookmarkedAt() {
        return bookmarkedAt;
    }
}
