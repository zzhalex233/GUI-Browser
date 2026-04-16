package com.zzhalex233.guibrowser.client.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;
import com.zzhalex233.guibrowser.client.session.TabSnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class TabPersistenceManager {

    private static final Logger LOGGER = Logger.getLogger(TabPersistenceManager.class.getName());

    private TabPersistenceManager() {
    }

    public static void save(List<GuiSession> sessions, File dataDir) {
        JsonArray arr = new JsonArray();
        for (GuiSession session : sessions) {
            GuiSessionSource source = session.getSource();
            if (source == null) continue;
            GuiSessionSourceKey key = GuiSessionSourceKey.fromSource(source);
            if (key == null) continue;
            TabSnapshot snapshot = new TabSnapshot(
                session.getTitle(),
                session.getScreen().getClass().getName(),
                key,
                session.getCreatedAt(),
                session.getLastActivatedAt()
            );
            arr.add(snapshot.toJson());
        }
        JsonPersistence.saveJson(new File(dataDir, "tabs.json"), arr);
    }

    public static void restoreAsStale(GuiSessionManager manager, File dataDir) {
        List<TabSnapshot> snapshots = load(dataDir);
        for (TabSnapshot snapshot : snapshots) {
            GuiSessionSourceKey key = snapshot.getSourceKey();
            GuiSessionSource source = toRuntimeSource(key);
            if (source == null) continue;
            manager.registerStaleSession(snapshot.getTitle(), source);
        }
    }

    public static List<TabSnapshot> load(File dataDir) {
        JsonElement element = JsonPersistence.loadJson(new File(dataDir, "tabs.json"));
        List<TabSnapshot> result = new ArrayList<>();
        if (!element.isJsonArray()) return result;
        for (JsonElement e : element.getAsJsonArray()) {
            TabSnapshot snapshot = TabSnapshot.fromJson(e);
            if (snapshot != null) {
                result.add(snapshot);
            }
        }
        return result;
    }

    private static GuiSessionSource toRuntimeSource(GuiSessionSourceKey key) {
        if (key instanceof GuiSessionSourceKey.BlockKey) {
            return ((GuiSessionSourceKey.BlockKey) key).toBlockSource();
        }
        // EntityKey cannot be converted without a live world to resolve UUID
        return null;
    }
}
