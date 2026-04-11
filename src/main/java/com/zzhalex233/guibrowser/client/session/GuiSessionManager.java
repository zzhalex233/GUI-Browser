package com.zzhalex233.guibrowser.client.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GuiSessionManager {
    private final Map<GuiSessionId, GuiSession> sessions = new LinkedHashMap<>();
    private GuiSessionId foregroundSessionId;
    private GuiSessionId lastActivatedSessionId;

    public GuiSession registerOpenedSession(Object screen, String title) {
        Objects.requireNonNull(screen, "screen");
        long now = System.currentTimeMillis();
        clearCurrentForeground();
        GuiSession session = new GuiSession(GuiSessionId.create(), screen, GuiSessionTitleResolver.resolve(screen, title), now);
        session.markForeground(now);
        sessions.put(session.getId(), session);
        foregroundSessionId = session.getId();
        lastActivatedSessionId = session.getId();
        return session;
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

    public GuiSession getForegroundSession() {
        return foregroundSessionId == null ? null : sessions.get(foregroundSessionId);
    }

    public GuiSessionId getLastActivatedSessionId() {
        return lastActivatedSessionId;
    }

    public void hideSession(GuiSessionId id) {
        GuiSession session = getSession(id);
        session.markHidden();
        if (id.equals(foregroundSessionId)) {
            foregroundSessionId = null;
        }
    }

    public void activateSession(GuiSessionId id) {
        GuiSession session = getSession(id);
        long now = System.currentTimeMillis();
        clearCurrentForeground();
        session.markForeground(now);
        foregroundSessionId = id;
        lastActivatedSessionId = id;
    }

    public void destroySession(GuiSessionId id) {
        sessions.remove(id);
        if (id.equals(foregroundSessionId)) {
            foregroundSessionId = null;
        }
        if (id.equals(lastActivatedSessionId)) {
            lastActivatedSessionId = null;
        }
    }

    public Collection<GuiSession> listVisibleTabs() {
        List<GuiSession> visible = new ArrayList<>();
        for (GuiSession session : sessions.values()) {
            if (!session.isHidden()) {
                visible.add(session);
            }
        }
        return Collections.unmodifiableList(visible);
    }

    public Collection<GuiSession> listAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    public void clearForWorldUnload() {
        sessions.clear();
        foregroundSessionId = null;
        lastActivatedSessionId = null;
    }

    private void clearCurrentForeground() {
        if (foregroundSessionId == null) {
            return;
        }
        GuiSession previous = sessions.get(foregroundSessionId);
        if (previous != null) {
            previous.clearForeground();
        }
        foregroundSessionId = null;
    }
}