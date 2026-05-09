package com.zzhalex233.guibrowser.client.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuiBookmarkFolder {

    private final String name;
    private final Map<String, GuiBookmarkFolder> children = new LinkedHashMap<>();
    private final List<GuiBookmarkEntry> entries = new ArrayList<>();

    public GuiBookmarkFolder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<GuiBookmarkEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public List<GuiBookmarkFolder> getChildren() {
        return Collections.unmodifiableList(new ArrayList<>(children.values()));
    }

    public GuiBookmarkFolder folder(String childName) {
        GuiBookmarkFolder folder = children.get(childName);
        if (folder == null) {
            folder = new GuiBookmarkFolder(childName);
            children.put(childName, folder);
        }
        return folder;
    }

    public GuiBookmarkFolder findFolder(String path) {
        if (path == null || path.trim().isEmpty()) {
            return this;
        }
        GuiBookmarkFolder cursor = this;
        for (String part : path.split("/")) {
            if (part.trim().isEmpty()) {
                continue;
            }
            cursor = cursor.children.get(part);
            if (cursor == null) {
                return null;
            }
        }
        return cursor;
    }

    public void addEntry(GuiBookmarkEntry entry) {
        entries.add(entry);
    }

    boolean removeEntry(GuiBookmarkEntry entry) {
        return entries.remove(entry);
    }

    void clear() {
        entries.clear();
        children.clear();
    }

    void collectEntries(List<GuiBookmarkEntry> output) {
        output.addAll(entries);
        for (GuiBookmarkFolder child : children.values()) {
            child.collectEntries(output);
        }
    }
}
