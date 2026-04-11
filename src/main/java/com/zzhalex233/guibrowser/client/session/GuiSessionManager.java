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
}