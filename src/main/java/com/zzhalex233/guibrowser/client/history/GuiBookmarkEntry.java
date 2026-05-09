package com.zzhalex233.guibrowser.client.history;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;

import javax.annotation.Nullable;

public final class GuiBookmarkEntry {

    private final GuiSessionSourceKey.BlockKey sourceKey;
    private String title;
    private String folderPath;
    private final String screenClassName;
    private long bookmarkedAt;

    public GuiBookmarkEntry(GuiSessionSourceKey.BlockKey sourceKey, String title, String screenClassName, long bookmarkedAt) {
        this(sourceKey, title, screenClassName, bookmarkedAt, "");
    }

    public GuiBookmarkEntry(GuiSessionSourceKey.BlockKey sourceKey, String title, String screenClassName, long bookmarkedAt, String folderPath) {
        this.sourceKey = sourceKey;
        this.title = title;
        this.screenClassName = screenClassName;
        this.bookmarkedAt = bookmarkedAt;
        this.folderPath = folderPath == null ? "" : folderPath;
    }

    public GuiSessionSourceKey.BlockKey getSourceKey() {
        return sourceKey;
    }

    public String getTitle() {
        return title;
    }

    public String getSessionTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSessionTitle(String title) {
        this.title = title;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath == null ? "" : folderPath;
    }

    public String getScreenClassName() {
        return screenClassName;
    }

    public long getBookmarkedAt() {
        return bookmarkedAt;
    }

    public String getDisplayLabel() {
        return title + " @ " + sourceKey.getX() + ", " + sourceKey.getY() + ", " + sourceKey.getZ()
            + " (dim " + sourceKey.getDimensionId() + ")";
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.add("source", sourceKey.toJson());
        obj.addProperty("title", title);
        obj.addProperty("folderPath", folderPath);
        obj.addProperty("screenClass", screenClassName);
        obj.addProperty("bookmarkedAt", bookmarkedAt);
        return obj;
    }

    @Nullable
    public static GuiBookmarkEntry fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        GuiSessionSourceKey key = obj.has("source") ? GuiSessionSourceKey.fromJson(obj.get("source")) : null;
        if (!(key instanceof GuiSessionSourceKey.BlockKey)) {
            return null;
        }
        String title = obj.has("title") ? obj.get("title").getAsString() : null;
        if (title == null) {
            return null;
        }
        String folderPath = obj.has("folderPath") ? obj.get("folderPath").getAsString() : "";
        String screenClass = obj.has("screenClass") ? obj.get("screenClass").getAsString() : "";
        long bookmarkedAt = obj.has("bookmarkedAt") ? obj.get("bookmarkedAt").getAsLong() : 0L;
        return new GuiBookmarkEntry((GuiSessionSourceKey.BlockKey) key, title, screenClass, bookmarkedAt, folderPath);
    }
}
