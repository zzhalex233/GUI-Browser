package com.zzhalex233.guibrowser.client.history;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zzhalex233.guibrowser.client.persistence.JsonPersistence;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuiHistoryStore {

    private static final int MAX_ENTRIES_PER_DIMENSION = 100;

    private final Map<Integer, LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry>> entriesByDimension = new LinkedHashMap<>();

    public void recordClosed(String sessionTitle, GuiSessionSourceKey.BlockKey sourceKey, long timestamp) {
        LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> dimensionEntries = entriesByDimensionFor(sourceKey.getDimensionId());
        GuiHistoryEntry entry = dimensionEntries.get(sourceKey);
        if (entry == null) {
            dimensionEntries.put(sourceKey, new GuiHistoryEntry(sourceKey, sessionTitle, timestamp, 1));
        } else {
            entry.refresh(sessionTitle, timestamp);
        }
        trimDimension(dimensionEntries);
    }

    public List<GuiHistoryEntry> entriesForDimension(int dimensionId) {
        LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> dimensionEntries = entriesByDimension.get(dimensionId);
        if (dimensionEntries == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(dimensionEntries.values()));
    }

    public GuiHistoryEntry findEntry(GuiSessionSourceKey.BlockKey key) {
        LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> dimensionEntries = entriesByDimension.get(key.getDimensionId());
        return dimensionEntries == null ? null : dimensionEntries.get(key);
    }

    public List<GuiHistoryEntry> entries() {
        List<GuiHistoryEntry> all = new ArrayList<>();
        for (LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> dimensionEntries : entriesByDimension.values()) {
            all.addAll(dimensionEntries.values());
        }
        return Collections.unmodifiableList(all);
    }

    public void remove(GuiSessionSourceKey.BlockKey key) {
        LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> dimensionEntries = entriesByDimension.get(key.getDimensionId());
        if (dimensionEntries == null) {
            return;
        }
        dimensionEntries.remove(key);
        if (dimensionEntries.isEmpty()) {
            entriesByDimension.remove(key.getDimensionId());
        }
    }

    public int size() {
        int size = 0;
        for (LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> dimensionEntries : entriesByDimension.values()) {
            size += dimensionEntries.size();
        }
        return size;
    }

    public void clear() {
        entriesByDimension.clear();
    }

    public void save(File dataDir) {
        JsonArray arr = new JsonArray();
        for (LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> dimensionEntries : entriesByDimension.values()) {
            for (GuiHistoryEntry entry : dimensionEntries.values()) {
                arr.add(entry.toJson());
            }
        }
        JsonPersistence.saveJson(new File(dataDir, "history.json"), arr);
    }

    public void load(File dataDir) {
        JsonElement element = JsonPersistence.loadJson(new File(dataDir, "history.json"));
        entriesByDimension.clear();
        if (!element.isJsonArray()) {
            return;
        }
        for (JsonElement e : element.getAsJsonArray()) {
            GuiHistoryEntry entry = GuiHistoryEntry.fromJson(e);
            if (entry != null) {
                entriesByDimensionFor(entry.getDimensionId()).put(entry.getSourceKey(), entry);
            }
        }
        for (LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> dimensionEntries : entriesByDimension.values()) {
            trimDimension(dimensionEntries);
        }
    }

    private LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> entriesByDimensionFor(int dimensionId) {
        LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> dimensionEntries = entriesByDimension.get(dimensionId);
        if (dimensionEntries == null) {
            dimensionEntries = new LinkedHashMap<>();
            entriesByDimension.put(dimensionId, dimensionEntries);
        }
        return dimensionEntries;
    }

    private void trimDimension(LinkedHashMap<GuiSessionSourceKey.BlockKey, GuiHistoryEntry> dimensionEntries) {
        while (dimensionEntries.size() > MAX_ENTRIES_PER_DIMENSION) {
            Iterator<GuiSessionSourceKey.BlockKey> iterator = dimensionEntries.keySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }
}
