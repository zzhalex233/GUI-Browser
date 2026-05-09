package com.zzhalex233.guibrowser.client.history;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;

import javax.annotation.Nullable;

public final class GuiHistoryEntry {

    private final GuiSessionSourceKey.BlockKey sourceKey;
    private String sessionTitle;
    private long closedAt;
    private int closeCount;

    public GuiHistoryEntry(GuiSessionSourceKey.BlockKey sourceKey, String sessionTitle, long closedAt, int closeCount) {
        this.sourceKey = sourceKey;
        this.sessionTitle = sessionTitle;
        this.closedAt = closedAt;
        this.closeCount = closeCount;
    }

    public GuiSessionSourceKey.BlockKey getSourceKey() {
        return sourceKey;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public long getClosedAt() {
        return closedAt;
    }

    public int getCloseCount() {
        return closeCount;
    }

    public void refresh(String newTitle, long timestamp) {
        this.sessionTitle = newTitle;
        this.closedAt = timestamp;
        this.closeCount++;
    }

    public int getDimensionId() {
        return sourceKey.getDimensionId();
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.add("source", sourceKey.toJson());
        obj.addProperty("title", sessionTitle);
        obj.addProperty("closedAt", closedAt);
        obj.addProperty("closeCount", closeCount);
        return obj;
    }

    @Nullable
    public static GuiHistoryEntry fromJson(JsonElement element) {
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
        long closedAt = obj.has("closedAt") ? obj.get("closedAt").getAsLong() : 0L;
        int closeCount = obj.has("closeCount") ? obj.get("closeCount").getAsInt() : 1;
        return new GuiHistoryEntry((GuiSessionSourceKey.BlockKey) key, title, closedAt, closeCount);
    }
}
