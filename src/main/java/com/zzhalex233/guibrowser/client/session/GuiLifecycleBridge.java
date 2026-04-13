package com.zzhalex233.guibrowser.client.session;

import com.zzhalex233.guibrowser.client.session.GuiTrackingPolicy.TrackingDecision;
import com.zzhalex233.guibrowser.config.ContainerCacheMode;
import net.minecraft.client.gui.GuiScreen;

import javax.annotation.Nullable;
import java.util.Objects;

public final class GuiLifecycleBridge {
    private final GuiSessionManager manager;
    @Nullable
    private final InteractionSourceTracker sourceTracker;
    private final ContainerCacheMode cacheMode;

    public GuiLifecycleBridge(GuiSessionManager manager) {
        this(manager, null, ContainerCacheMode.HYBRID);
    }

    public GuiLifecycleBridge(GuiSessionManager manager, @Nullable InteractionSourceTracker sourceTracker, ContainerCacheMode cacheMode) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.sourceTracker = sourceTracker;
        this.cacheMode = Objects.requireNonNull(cacheMode, "cacheMode");
    }

    public TransitionDecision onBeforeDisplay(@Nullable GuiScreen current, @Nullable GuiScreen incoming, boolean explicitDestroy) {
        if (current != null && current == incoming) {
            GuiSession currentSession = manager.findSessionByScreen(current);
            GuiSessionId currentSessionId = currentSession == null ? null : currentSession.getId();
            return new TransitionDecision(false, null, currentSessionId);
        }

        // Handle outgoing screen
        GuiSessionId hiddenSessionId = null;
        boolean suppressCurrentClose = false;

        if (current != null) {
            GuiSession currentSession = manager.findSessionByScreen(current);
            if (currentSession != null) {
                if (explicitDestroy) {
                    manager.destroySession(currentSession.getId());
                } else {
                    manager.hideSession(currentSession.getId());
                    currentSession.markStale();
                    hiddenSessionId = currentSession.getId();
                    suppressCurrentClose = cacheMode == ContainerCacheMode.HYBRID;
                }
            }
        }

        // Handle incoming screen
        GuiSessionId activatedSessionId = null;
        if (incoming != null) {
            boolean hasSource = sourceTracker != null && sourceTracker.hasPending();
            TrackingDecision decision = GuiTrackingPolicy.decide(incoming, hasSource);

            if (decision == TrackingDecision.TRACK_AS_TAB) {
                GuiSessionSource source = null;
                if (sourceTracker != null) {
                    long currentTick = System.currentTimeMillis() / 50;
                    source = sourceTracker.consumePending(currentTick);
                }

                GuiSession incomingSession = manager.registerOrReuseSession(incoming, null, source);
                activatedSessionId = incomingSession.getId();
            }
        }

        return new TransitionDecision(suppressCurrentClose, hiddenSessionId, activatedSessionId);
    }

    public void onAfterDisplay(@Nullable GuiScreen nowVisible) {
        if (nowVisible == null) {
            return;
        }

        GuiSession session = manager.findSessionByScreen(nowVisible);
        if (session != null) {
            if (!session.isForeground()) {
                manager.activateSession(session.getId());
            }
            return;
        }

        boolean hasSource = sourceTracker != null && sourceTracker.hasPending();
        TrackingDecision decision = GuiTrackingPolicy.decide(nowVisible, hasSource);

        if (decision == TrackingDecision.TRACK_AS_TAB) {
            GuiSessionSource source = null;
            if (sourceTracker != null) {
                long currentTick = System.currentTimeMillis() / 50;
                source = sourceTracker.consumePending(currentTick);
            }
            manager.registerOrReuseSession(nowVisible, null, source);
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
