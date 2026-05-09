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

    private final GuiBookmarkFolder rootFolder = new GuiBookmarkFolder("");
    private final Map<GuiSessionSourceKey.BlockKey, GuiBookmarkEntry> entryIndex = new LinkedHashMap<>();

    public GuiBookmarkFolder getRootFolder() {
        return rootFolder;
    }

    public void addFolderPath(String path) {
        ensureFolder(path);
    }

    public void addBookmark(String folderPath, GuiSessionSourceKey.BlockKey key, String title, String screenClassName) {
        remove(key);
        GuiBookmarkEntry entry = new GuiBookmarkEntry(key, title, screenClassName, System.currentTimeMillis(), normalizePath(folderPath));
        entryIndex.put(key, entry);
        ensureFolder(folderPath).addEntry(entry);
    }

    public void addBookmark(GuiSessionSourceKey key, String title, String screenClassName) {
        if (key instanceof GuiSessionSourceKey.BlockKey) {
            addBookmark("", (GuiSessionSourceKey.BlockKey) key, title, screenClassName);
        }
    }

    public GuiBookmarkEntry findEntry(GuiSessionSourceKey.BlockKey key) {
        return entryIndex.get(key);
    }

    public GuiBookmarkEntry getBookmark(GuiSessionSourceKey key) {
        return key instanceof GuiSessionSourceKey.BlockKey ? entryIndex.get(key) : null;
    }

    public boolean isBookmarked(GuiSessionSourceKey.BlockKey key) {
        return entryIndex.containsKey(key);
    }

    public boolean isBookmarked(GuiSessionSourceKey key) {
        return key instanceof GuiSessionSourceKey.BlockKey && isBookmarked((GuiSessionSourceKey.BlockKey) key);
    }

    public void updateTitle(GuiSessionSourceKey.BlockKey key, String newTitle) {
        GuiBookmarkEntry entry = entryIndex.get(key);
        if (entry != null) {
            entry.setTitle(newTitle);
        }
    }

    public void updateTitle(GuiSessionSourceKey key, String newTitle) {
        if (key instanceof GuiSessionSourceKey.BlockKey) {
            updateTitle((GuiSessionSourceKey.BlockKey) key, newTitle);
        }
    }

    public void remove(GuiSessionSourceKey.BlockKey key) {
        GuiBookmarkEntry entry = entryIndex.remove(key);
        if (entry == null) {
            return;
        }
        removeFromFolder(rootFolder, entry);
    }

    public void moveBookmark(GuiSessionSourceKey.BlockKey key, String folderPath) {
        GuiBookmarkEntry entry = entryIndex.get(key);
        if (entry == null) {
            return;
        }
        removeFromFolder(rootFolder, entry);
        entry.setFolderPath(normalizePath(folderPath));
        ensureFolder(folderPath).addEntry(entry);
    }

    public List<String> folderPaths() {
        List<String> paths = new ArrayList<>();
        paths.add("");
        collectFolderPaths(rootFolder, "", paths);
        return Collections.unmodifiableList(paths);
    }

    public void removeBookmark(GuiSessionSourceKey key) {
        if (key instanceof GuiSessionSourceKey.BlockKey) {
            remove((GuiSessionSourceKey.BlockKey) key);
        }
    }

    public void toggleBookmark(GuiSessionSourceKey key, String title, String screenClassName) {
        if (!(key instanceof GuiSessionSourceKey.BlockKey)) {
            return;
        }
        GuiSessionSourceKey.BlockKey blockKey = (GuiSessionSourceKey.BlockKey) key;
        if (isBookmarked(blockKey)) {
            remove(blockKey);
        } else {
            addBookmark("", blockKey, title, screenClassName);
        }
    }

    public List<GuiBookmarkEntry> allEntries() {
        List<GuiBookmarkEntry> entries = new ArrayList<>();
        rootFolder.collectEntries(entries);
        return Collections.unmodifiableList(entries);
    }

    public int size() {
        return entryIndex.size();
    }

    public void clear() {
        entryIndex.clear();
        rootFolder.clear();
    }

    public void save(File dataDir) {
        JsonArray arr = new JsonArray();
        for (GuiBookmarkEntry entry : entryIndex.values()) {
            arr.add(entry.toJson());
        }
        JsonPersistence.saveJson(new File(dataDir, "bookmarks.json"), arr);
    }

    public void load(File dataDir) {
        JsonElement element = JsonPersistence.loadJson(new File(dataDir, "bookmarks.json"));
        clear();
        if (!element.isJsonArray()) {
            return;
        }
        for (JsonElement e : element.getAsJsonArray()) {
            GuiBookmarkEntry entry = GuiBookmarkEntry.fromJson(e);
            if (entry != null) {
                entryIndex.put(entry.getSourceKey(), entry);
                ensureFolder(entry.getFolderPath()).addEntry(entry);
            }
        }
    }

    private GuiBookmarkFolder ensureFolder(String path) {
        GuiBookmarkFolder folder = rootFolder;
        if (path == null || path.trim().isEmpty()) {
            return folder;
        }
        for (String part : path.split("/")) {
            if (part.trim().isEmpty()) {
                continue;
            }
            folder = folder.folder(part.trim());
        }
        return folder;
    }

    private void removeFromFolder(GuiBookmarkFolder folder, GuiBookmarkEntry entry) {
        if (folder.removeEntry(entry)) {
            return;
        }
        for (GuiBookmarkFolder child : folder.getChildren()) {
            removeFromFolder(child, entry);
        }
    }

    private void collectFolderPaths(GuiBookmarkFolder folder, String prefix, List<String> paths) {
        for (GuiBookmarkFolder child : folder.getChildren()) {
            String path = prefix.isEmpty() ? child.getName() : prefix + "/" + child.getName();
            paths.add(path);
            collectFolderPaths(child, path, paths);
        }
    }

    private String normalizePath(String path) {
        return path == null ? "" : path.trim();
    }
}
