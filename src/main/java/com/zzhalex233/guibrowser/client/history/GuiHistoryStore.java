package com.zzhalex233.guibrowser.client.history;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zzhalex233.guibrowser.client.persistence.JsonPersistence;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

public final class GuiHistoryStore {

    private static final int MAX_ENTRIES = 500;

    private final List<GuiHistoryEntry> entries = new ArrayList<>();

    public void record(String sessionTitle, GuiHistoryEntry.Action action) {
        record(sessionTitle, action, null);
    }

    public void record(String sessionTitle, GuiHistoryEntry.Action action,
                       @Nullable GuiSessionSourceKey sourceKey) {
        entries.add(new GuiHistoryEntry(sessionTitle, action, System.currentTimeMillis(), sourceKey));
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
    }

    public List<GuiHistoryEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    public void save(File dataDir) {
        JsonArray arr = new JsonArray();
        for (GuiHistoryEntry entry : entries) {
            arr.add(entry.toJson());
        }
        JsonPersistence.saveJson(new File(dataDir, "history.json"), arr);
    }

    public void load(File dataDir) {
        JsonElement element = JsonPersistence.loadJson(new File(dataDir, "history.json"));
        if (!element.isJsonArray()) return;
        entries.clear();
        for (JsonElement e : element.getAsJsonArray()) {
            GuiHistoryEntry entry = GuiHistoryEntry.fromJson(e);
            if (entry != null) {
                entries.add(entry);
            }
        }
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
    }
}
