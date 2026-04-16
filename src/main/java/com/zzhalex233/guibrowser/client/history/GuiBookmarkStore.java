package com.zzhalex233.guibrowser.client.history;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zzhalex233.guibrowser.client.persistence.JsonPersistence;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuiBookmarkStore {

    private final LinkedHashMap<GuiSessionSourceKey, GuiBookmarkEntry> bookmarks = new LinkedHashMap<>();

    public void addBookmark(GuiSessionSourceKey key, String title, String screenClassName) {
        bookmarks.put(key, new GuiBookmarkEntry(title, screenClassName, System.currentTimeMillis(), key));
    }

    public void removeBookmark(GuiSessionSourceKey key) {
        bookmarks.remove(key);
    }

    public boolean isBookmarked(GuiSessionSourceKey key) {
        return bookmarks.containsKey(key);
    }

    public GuiBookmarkEntry getBookmark(GuiSessionSourceKey key) {
        return bookmarks.get(key);
    }

    public void updateTitle(GuiSessionSourceKey key, String newTitle) {
        GuiBookmarkEntry entry = bookmarks.get(key);
        if (entry != null) {
            entry.setSessionTitle(newTitle);
        }
    }

    public void toggleBookmark(GuiSessionSourceKey key, String title, String screenClassName) {
        if (isBookmarked(key)) {
            removeBookmark(key);
        } else {
            addBookmark(key, title, screenClassName);
        }
    }

    public Map<GuiSessionSourceKey, GuiBookmarkEntry> allBookmarks() {
        return Collections.unmodifiableMap(bookmarks);
    }

    public List<GuiBookmarkEntry> allEntries() {
        return Collections.unmodifiableList(new ArrayList<>(bookmarks.values()));
    }

    public int size() {
        return bookmarks.size();
    }

    public void clear() {
        bookmarks.clear();
    }

    public void save(File dataDir) {
        JsonArray arr = new JsonArray();
        for (GuiBookmarkEntry entry : bookmarks.values()) {
            arr.add(entry.toJson());
        }
        JsonPersistence.saveJson(new File(dataDir, "bookmarks.json"), arr);
    }

    public void load(File dataDir) {
        JsonElement element = JsonPersistence.loadJson(new File(dataDir, "bookmarks.json"));
        if (!element.isJsonArray()) return;
        bookmarks.clear();
        for (JsonElement e : element.getAsJsonArray()) {
            GuiBookmarkEntry entry = GuiBookmarkEntry.fromJson(e);
            if (entry != null) {
                bookmarks.put(entry.getSourceKey(), entry);
            }
        }
    }
}
