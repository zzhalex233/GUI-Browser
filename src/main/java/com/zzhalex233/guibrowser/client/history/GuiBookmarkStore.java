package com.zzhalex233.guibrowser.client.history;

import com.zzhalex233.guibrowser.client.session.GuiSessionId;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class GuiBookmarkStore {

    private final LinkedHashMap<GuiSessionId, GuiBookmarkEntry> bookmarks = new LinkedHashMap<>();

    public void addBookmark(GuiSessionId sessionId, String title, String screenClassName) {
        bookmarks.put(sessionId, new GuiBookmarkEntry(title, screenClassName, System.currentTimeMillis()));
    }

    public void removeBookmark(GuiSessionId sessionId) {
        bookmarks.remove(sessionId);
    }

    public boolean isBookmarked(GuiSessionId sessionId) {
        return bookmarks.containsKey(sessionId);
    }

    public void toggleBookmark(GuiSessionId sessionId, String title, String screenClassName) {
        if (isBookmarked(sessionId)) {
            removeBookmark(sessionId);
        } else {
            addBookmark(sessionId, title, screenClassName);
        }
    }

    public Map<GuiSessionId, GuiBookmarkEntry> allBookmarks() {
        return Collections.unmodifiableMap(bookmarks);
    }

    public Set<GuiSessionId> bookmarkedIds() {
        return Collections.unmodifiableSet(bookmarks.keySet());
    }

    public int size() {
        return bookmarks.size();
    }

    public void clear() {
        bookmarks.clear();
    }
}
