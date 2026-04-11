package com.zzhalex233.guibrowser.client.session;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class GuiSessionManager {
    private final Map<GuiSessionId, GuiSession> sessions = new LinkedHashMap<>();
    private GuiSessionId foregroundSessionId;

    public GuiSession registerOpenedSession(Object screen, String title) {
        Objects.requireNonNull(screen, "screen");
        long now = System.currentTimeMillis();
        if (foregroundSessionId != null) {
            GuiSession previous = sessions.get(foregroundSessionId);
            if (previous != null) {
                previous.clearForeground();
            }
        }
        GuiSession session = new GuiSession(GuiSessionId.create(), screen, GuiSessionTitleResolver.resolve(screen, title), now);
        session.markForeground(now);
        sessions.put(session.getId(), session);
        foregroundSessionId = session.getId();
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

    public void hideSession(GuiSessionId id) {
        GuiSession session = getSession(id);
        session.markHidden();
        if (id.equals(foregroundSessionId)) {
            foregroundSessionId = null;
        }
    }

    public void destroySession(GuiSessionId id) {
        sessions.remove(id);
        if (id.equals(foregroundSessionId)) {
            foregroundSessionId = null;
        }
    }

    public Collection<GuiSession> listVisibleTabs() {
        return Collections.unmodifiableCollection(sessions.values());
    }
}