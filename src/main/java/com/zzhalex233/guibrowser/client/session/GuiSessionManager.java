package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public final class GuiSessionManager {
    private final LinkedHashMap<GuiSessionId, GuiSession> sessions = new LinkedHashMap<>();
    private GuiSessionId foregroundSessionId;
    private GuiSessionId lastActivatedSessionId;

    public GuiSession registerOpenedSession(GuiScreen screen, String title) {
        Objects.requireNonNull(screen, "screen");
        long now = System.currentTimeMillis();
        GuiSession previousForeground = getForegroundSession();
        if (previousForeground != null) {
            previousForeground.clearForeground();
        }
        GuiSession session = new GuiSession(GuiSessionId.create(), screen, GuiSessionTitleResolver.resolve(screen, title), now);
        session.markForeground(now);
        sessions.put(session.getId(), session);
        foregroundSessionId = session.getId();
        lastActivatedSessionId = session.getId();
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

    public GuiSessionId getLastActivatedSessionId() {
        return lastActivatedSessionId;
    }

    public void hideSession(GuiSessionId id) {
        GuiSession session = requireSession(id);
        session.markHidden();
        if (id.equals(foregroundSessionId)) {
            foregroundSessionId = null;
        }
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
    }

    public void destroySession(GuiSessionId id) {
        GuiSession removed = sessions.remove(id);
        if (removed == null) {
            return;
        }
        if (id.equals(foregroundSessionId)) {
            foregroundSessionId = null;
        }
        if (id.equals(lastActivatedSessionId)) {
            lastActivatedSessionId = findMostRecentlyActivatedSessionId();
        }
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

    public void clearForWorldUnload() {
        sessions.clear();
        foregroundSessionId = null;
        lastActivatedSessionId = null;
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
}
