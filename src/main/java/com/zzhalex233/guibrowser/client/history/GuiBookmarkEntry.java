package com.zzhalex233.guibrowser.client.history;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;

import javax.annotation.Nullable;

public final class GuiBookmarkEntry {

    private String sessionTitle;
    private final String screenClassName;
    private final long bookmarkedAt;
    private final GuiSessionSourceKey sourceKey;

    public GuiBookmarkEntry(String sessionTitle, String screenClassName, long bookmarkedAt,
                            GuiSessionSourceKey sourceKey) {
        this.sessionTitle = sessionTitle;
        this.screenClassName = screenClassName;
        this.bookmarkedAt = bookmarkedAt;
        this.sourceKey = sourceKey;
    }

    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String title) { this.sessionTitle = title; }
    public String getScreenClassName() { return screenClassName; }
    public long getBookmarkedAt() { return bookmarkedAt; }
    public GuiSessionSourceKey getSourceKey() { return sourceKey; }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("title", sessionTitle);
        obj.addProperty("screenClass", screenClassName);
        obj.addProperty("bookmarkedAt", bookmarkedAt);
        obj.add("source", sourceKey.toJson());
        return obj;
    }

    @Nullable
    public static GuiBookmarkEntry fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        String title = obj.has("title") ? obj.get("title").getAsString() : null;
        String screenClass = obj.has("screenClass") ? obj.get("screenClass").getAsString() : "";
        long bookmarkedAt = obj.has("bookmarkedAt") ? obj.get("bookmarkedAt").getAsLong() : 0;
        GuiSessionSourceKey key = obj.has("source") ? GuiSessionSourceKey.fromJson(obj.get("source")) : null;
        if (title == null || key == null) return null;
        return new GuiBookmarkEntry(title, screenClass, bookmarkedAt, key);
    }
}
