package com.zzhalex233.guibrowser.client.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GuiHistoryStore {

    private final List<GuiHistoryEntry> entries = new ArrayList<>();

    public void record(String sessionTitle, GuiHistoryEntry.Action action) {
        entries.add(new GuiHistoryEntry(sessionTitle, action, System.currentTimeMillis()));
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
}
