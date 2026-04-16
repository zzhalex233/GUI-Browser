package com.zzhalex233.guibrowser.client.session;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;

public final class TabSnapshot {

    private final String title;
    private final String screenClassName;
    private final GuiSessionSourceKey sourceKey;
    private final long createdAt;
    private final long lastActivatedAt;

    public TabSnapshot(String title, String screenClassName, GuiSessionSourceKey sourceKey,
                       long createdAt, long lastActivatedAt) {
        this.title = title;
        this.screenClassName = screenClassName;
        this.sourceKey = sourceKey;
        this.createdAt = createdAt;
        this.lastActivatedAt = lastActivatedAt;
    }

    public String getTitle() { return title; }
    public String getScreenClassName() { return screenClassName; }
    public GuiSessionSourceKey getSourceKey() { return sourceKey; }
    public long getCreatedAt() { return createdAt; }
    public long getLastActivatedAt() { return lastActivatedAt; }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("title", title);
        obj.addProperty("screenClass", screenClassName);
        obj.add("source", sourceKey.toJson());
        obj.addProperty("createdAt", createdAt);
        obj.addProperty("lastActivatedAt", lastActivatedAt);
        return obj;
    }

    @Nullable
    public static TabSnapshot fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        String title = obj.has("title") ? obj.get("title").getAsString() : null;
        String screenClass = obj.has("screenClass") ? obj.get("screenClass").getAsString() : null;
        GuiSessionSourceKey sourceKey = obj.has("source")
            ? GuiSessionSourceKey.fromJson(obj.get("source")) : null;
        if (title == null || sourceKey == null) {
            return null;
        }
        long createdAt = obj.has("createdAt") ? obj.get("createdAt").getAsLong() : 0;
        long lastActivatedAt = obj.has("lastActivatedAt") ? obj.get("lastActivatedAt").getAsLong() : createdAt;
        return new TabSnapshot(title, screenClass != null ? screenClass : "", sourceKey, createdAt, lastActivatedAt);
    }
}
