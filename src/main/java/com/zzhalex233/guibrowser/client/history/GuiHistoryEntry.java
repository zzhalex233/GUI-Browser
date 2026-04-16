package com.zzhalex233.guibrowser.client.history;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;

import javax.annotation.Nullable;

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
    @Nullable
    private final GuiSessionSourceKey sourceKey;

    public GuiHistoryEntry(String sessionTitle, Action action, long timestamp) {
        this(sessionTitle, action, timestamp, null);
    }

    public GuiHistoryEntry(String sessionTitle, Action action, long timestamp,
                           @Nullable GuiSessionSourceKey sourceKey) {
        this.sessionTitle = sessionTitle;
        this.action = action;
        this.timestamp = timestamp;
        this.sourceKey = sourceKey;
    }

    public String getSessionTitle() { return sessionTitle; }
    public Action getAction() { return action; }
    public long getTimestamp() { return timestamp; }
    @Nullable
    public GuiSessionSourceKey getSourceKey() { return sourceKey; }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("title", sessionTitle);
        obj.addProperty("action", action.name());
        obj.addProperty("timestamp", timestamp);
        if (sourceKey != null) {
            obj.add("source", sourceKey.toJson());
        }
        return obj;
    }

    @Nullable
    public static GuiHistoryEntry fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        String title = obj.has("title") ? obj.get("title").getAsString() : null;
        if (title == null) return null;
        Action action;
        try {
            action = Action.valueOf(obj.get("action").getAsString());
        } catch (Exception e) {
            return null;
        }
        long timestamp = obj.has("timestamp") ? obj.get("timestamp").getAsLong() : 0;
        GuiSessionSourceKey sourceKey = obj.has("source")
            ? GuiSessionSourceKey.fromJson(obj.get("source")) : null;
        return new GuiHistoryEntry(title, action, timestamp, sourceKey);
    }
}
