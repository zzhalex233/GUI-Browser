package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;

import javax.annotation.Nullable;
import java.util.Objects;

public final class GuiLifecycleBridge {
    private static final GuiLifecycleBridge INSTANCE = new GuiLifecycleBridge(new GuiSessionManager());

    private final GuiSessionManager manager;

    public GuiLifecycleBridge(GuiSessionManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public static GuiLifecycleBridge getInstance() {
        return INSTANCE;
    }

    public TransitionDecision onBeforeDisplay(@Nullable GuiScreen current, @Nullable GuiScreen incoming, boolean explicitDestroy) {
        if (current != null && current == incoming) {
            GuiSession currentSession = ensureTrackedSession(current);
            GuiSessionId currentSessionId = currentSession == null ? null : currentSession.getId();
            return new TransitionDecision(false, null, currentSessionId);
        }

        GuiSessionId hiddenSessionId = null;
        boolean suppressCurrentClose = false;
        if (GuiTrackingPolicy.shouldTrack(current)) {
            GuiSession currentSession = ensureTrackedSession(current);
            if (currentSession != null) {
                if (explicitDestroy) {
                    manager.destroySession(currentSession.getId());
                } else {
                    manager.hideSession(currentSession.getId());
                    hiddenSessionId = currentSession.getId();
                    suppressCurrentClose = true;
                }
            }
        }

        GuiSessionId activatedSessionId = null;
        if (GuiTrackingPolicy.shouldTrack(incoming)) {
            GuiSession incomingSession = manager.findSessionByScreen(incoming);
            if (incomingSession == null) {
                incomingSession = manager.registerOpenedSession(incoming, null);
            } else {
                manager.activateSession(incomingSession.getId());
                incomingSession = manager.getSession(incomingSession.getId());
            }
            activatedSessionId = incomingSession.getId();
        }

        return new TransitionDecision(suppressCurrentClose, hiddenSessionId, activatedSessionId);
    }

    public void onAfterDisplay(@Nullable GuiScreen nowVisible) {
        if (!GuiTrackingPolicy.shouldTrack(nowVisible)) {
            return;
        }

        GuiSession session = manager.findSessionByScreen(nowVisible);
        if (session == null) {
            manager.registerOpenedSession(nowVisible, null);
            return;
        }

        if (!session.isForeground()) {
            manager.activateSession(session.getId());
        }
    }

    public void onWorldUnload() {
        manager.clearForWorldUnload();
    }

    public void clearForWorldUnload() {
        onWorldUnload();
    }

    public void destroySessionFromTab(GuiSessionId id) {
        manager.destroySession(id);
    }

    public void destroyForegroundSession() {
        GuiSession foreground = manager.getForegroundSession();
        if (foreground != null) {
            manager.destroySession(foreground.getId());
        }
    }

    @Nullable
    private GuiSession ensureTrackedSession(@Nullable GuiScreen screen) {
        if (!GuiTrackingPolicy.shouldTrack(screen)) {
            return null;
        }

        GuiSession existing = manager.findSessionByScreen(screen);
        if (existing != null) {
            return existing;
        }

        return manager.registerOpenedSession(screen, null);
    }

    public static final class TransitionDecision {
        private final boolean suppressCurrentClose;
        private final GuiSessionId hiddenSessionId;
        private final GuiSessionId activatedSessionId;

        private TransitionDecision(boolean suppressCurrentClose, @Nullable GuiSessionId hiddenSessionId, @Nullable GuiSessionId activatedSessionId) {
            this.suppressCurrentClose = suppressCurrentClose;
            this.hiddenSessionId = hiddenSessionId;
            this.activatedSessionId = activatedSessionId;
        }

        public boolean shouldSuppressCurrentClose() {
            return suppressCurrentClose;
        }

        @Nullable
        public GuiSessionId getHiddenSessionId() {
            return hiddenSessionId;
        }

        @Nullable
        public GuiSessionId getActivatedSessionId() {
            return activatedSessionId;
        }
    }
}
