package com.zzhalex233.guibrowser.client.session;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class GuiSessionManager {
    private final Map<GuiSessionId, GuiSession> sessions = new LinkedHashMap<>();
    private GuiSessionId foregroundSessionId;

    public GuiSession registerOpenedSession(Object screen, String title) {
        Objects.requireNonNull(screen, "screen");
        long now = System.currentTimeMillis();
        clearCurrentForeground();
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

    public GuiSession getForegroundSession() {
        return foregroundSessionId == null ? null : sessions.get(foregroundSessionId);
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
