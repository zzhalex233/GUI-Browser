package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;

import com.zzhalex233.guibrowser.client.history.GuiBookmarkStore;
import com.zzhalex233.guibrowser.client.history.GuiHistoryEntry;
import com.zzhalex233.guibrowser.client.history.GuiHistoryStore;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

public final class GuiSessionManager {
    private final LinkedHashMap<GuiSessionId, GuiSession> sessions = new LinkedHashMap<>();
    private final HashMap<GuiSessionSource, GuiSessionId> sourceIndex = new HashMap<>();
    private final GuiHistoryStore historyStore;
    private final GuiBookmarkStore bookmarkStore;
    private GuiSessionId foregroundSessionId;
    private GuiSessionId lastActivatedSessionId;
    private GuiSessionId lastServerWindowSessionId;

    public GuiSessionManager() {
        this(null, null);
    }

    public GuiSessionManager(GuiHistoryStore historyStore) {
        this(historyStore, null);
    }

    public GuiSessionManager(GuiHistoryStore historyStore, GuiBookmarkStore bookmarkStore) {
        this.historyStore = historyStore;
        this.bookmarkStore = bookmarkStore;
    }

    public GuiSession registerOpenedSession(GuiScreen screen, String title) {
        return registerOrReuseSession(screen, title, null);
    }

    public GuiSession registerOrReuseSession(GuiScreen screen, String title, @Nullable GuiSessionSource source) {
        Objects.requireNonNull(screen, "screen");
        long now = System.currentTimeMillis();

        if (source != null) {
            GuiSessionId existingId = sourceIndex.get(source);
            if (existingId != null) {
                GuiSession existing = sessions.get(existingId);
                if (existing != null) {
                    activateSession(existingId);
                    existing.updateScreen(screen);
                    existing.clearStale();
                    return existing;
                }
                sourceIndex.remove(source);
            }
        }

        GuiSession previousForeground = getForegroundSession();
        if (previousForeground != null) {
            previousForeground.clearForeground();
        }
        GuiSession session = new GuiSession(GuiSessionId.create(), screen, GuiSessionTitleResolver.resolve(screen, title), now, source);
        session.markForeground(now);
        sessions.put(session.getId(), session);
        foregroundSessionId = session.getId();
        lastActivatedSessionId = session.getId();

        if (source != null) {
            sourceIndex.put(source, session.getId());
        }

        recordHistory(session.getTitle(), GuiHistoryEntry.Action.OPENED, session.getSource());
        return session;
    }

    public GuiSession registerStaleSession(String title, GuiSessionSource source) {
        Objects.requireNonNull(source, "source");
        // Skip if a session with this source already exists
        GuiSessionId existingId = sourceIndex.get(source);
        if (existingId != null && sessions.containsKey(existingId)) {
            return sessions.get(existingId);
        }
        long now = System.currentTimeMillis();
        GuiScreen placeholder = new StaleTabPlaceholderScreen(title);
        GuiSession session = new GuiSession(GuiSessionId.create(), placeholder, title, now, source);
        session.markStale();
        session.markHidden();
        sessions.put(session.getId(), session);
        sourceIndex.put(source, session.getId());
        return session;
    }

    public GuiSession getForegroundSession() {
        return foregroundSessionId == null ? null : sessions.get(foregroundSessionId);
    }

    public GuiSession getSession(GuiSessionId id) {
        GuiSession session = sessions.get(id);
        if (session == null) {
            throw new IllegalArgumentException("Unknown session id: " + id);
        }
        return session;
    }

    public GuiSession findSession(GuiSessionId id) {
        return sessions.get(id);
    }

    public GuiSession findSessionByScreen(GuiScreen screen) {
        for (GuiSession session : sessions.values()) {
            if (session.getScreen() == screen) {
                return session;
            }
        }
        return null;
    }

    public GuiSessionId getLastActivatedSessionId() {
        return lastActivatedSessionId;
    }

    @Nullable
    public GuiSessionId getLastServerWindowSessionId() {
        return lastServerWindowSessionId;
    }

    public void setLastServerWindowSessionId(@Nullable GuiSessionId id) {
        this.lastServerWindowSessionId = id;
    }

    public void hideSession(GuiSessionId id) {
        GuiSession session = requireSession(id);
        session.markHidden();
        if (id.equals(foregroundSessionId)) {
            foregroundSessionId = null;
        }
        recordHistory(session.getTitle(), GuiHistoryEntry.Action.HIDDEN, session.getSource());
    }

    public void activateSession(GuiSessionId id) {
        GuiSession session = requireSession(id);
        long now = System.currentTimeMillis();
        GuiSession currentForeground = getForegroundSession();
        if (currentForeground != null && !currentForeground.getId().equals(id)) {
            currentForeground.clearForeground();
        }
        session.markForeground(now);
        foregroundSessionId = id;
        lastActivatedSessionId = id;
        recordHistory(session.getTitle(), GuiHistoryEntry.Action.ACTIVATED, session.getSource());
    }

    public void destroySession(GuiSessionId id) {
        GuiSession removed = sessions.remove(id);
        if (removed == null) {
            return;
        }
        if (removed.getSource() != null) {
            sourceIndex.remove(removed.getSource());
        }
        if (id.equals(foregroundSessionId)) {
            foregroundSessionId = null;
        }
        if (id.equals(lastServerWindowSessionId)) {
            lastServerWindowSessionId = null;
        }
        if (id.equals(lastActivatedSessionId)) {
            lastActivatedSessionId = findMostRecentlyActivatedSessionId();
        }
        recordHistory(removed.getTitle(), GuiHistoryEntry.Action.DESTROYED, removed.getSource());
    }

    public List<GuiSession> listVisibleTabs() {
        List<GuiSession> visible = new ArrayList<>();
        for (GuiSession session : sessions.values()) {
            if (!session.isHidden()) {
                visible.add(session);
            }
        }
        return visible;
    }

    public List<GuiSession> listAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    public void clearForWorldUnload() {
        recordHistory("*", GuiHistoryEntry.Action.CLEARED_ON_UNLOAD);
        sessions.clear();
        sourceIndex.clear();
        foregroundSessionId = null;
        lastActivatedSessionId = null;
        lastServerWindowSessionId = null;
        if (bookmarkStore != null) {
            bookmarkStore.clear();
        }
    }

    public void toggleBookmark(GuiSessionId id) {
        if (bookmarkStore == null) {
            return;
        }
        GuiSession session = requireSession(id);
        GuiSessionSource source = session.getSource();
        if (source == null) {
            return;
        }
        GuiSessionSourceKey key = GuiSessionSourceKey.fromSource(source);
        if (key == null) {
            return;
        }
        bookmarkStore.toggleBookmark(key, session.getTitle(), session.getScreen().getClass().getName());
    }

    public boolean isSessionBookmarked(GuiSessionId id) {
        if (bookmarkStore == null) return false;
        GuiSession session = findSession(id);
        if (session == null) return false;
        GuiSessionSource source = session.getSource();
        if (source == null) return false;
        GuiSessionSourceKey key = GuiSessionSourceKey.fromSource(source);
        return key != null && bookmarkStore.isBookmarked(key);
    }

    public GuiBookmarkStore getBookmarkStore() {
        return bookmarkStore;
    }

    public GuiHistoryStore getHistoryStore() {
        return historyStore;
    }

    private GuiSessionId findMostRecentlyActivatedSessionId() {
        GuiSessionId mostRecentlyActivatedSessionId = null;
        long mostRecentActivation = Long.MIN_VALUE;
        for (GuiSession session : sessions.values()) {
            long lastActivatedAt = session.getLastActivatedAt();
            if (mostRecentlyActivatedSessionId == null || lastActivatedAt >= mostRecentActivation) {
                mostRecentActivation = lastActivatedAt;
                mostRecentlyActivatedSessionId = session.getId();
            }
        }
        return mostRecentlyActivatedSessionId;
    }

    private GuiSession requireSession(GuiSessionId id) {
        GuiSession session = sessions.get(id);
        if (session == null) {
            throw new IllegalArgumentException("Unknown session id: " + id);
        }
        return session;
    }

    private void recordHistory(String title, GuiHistoryEntry.Action action) {
        recordHistory(title, action, null);
    }

    private void recordHistory(String title, GuiHistoryEntry.Action action, @Nullable GuiSessionSource source) {
        if (historyStore != null) {
            GuiSessionSourceKey sourceKey = source != null ? GuiSessionSourceKey.fromSource(source) : null;
            historyStore.record(title, action, sourceKey);
        }
    }
}
