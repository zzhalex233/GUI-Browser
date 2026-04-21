package com.zzhalex233.guibrowser.client.session;

import com.zzhalex233.guibrowser.client.session.GuiTrackingPolicy.TrackingDecision;
import com.zzhalex233.guibrowser.client.persistence.TabPersistenceManager;
import com.zzhalex233.guibrowser.config.ContainerCacheMode;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Objects;

public final class GuiLifecycleBridge {
    private final GuiSessionManager manager;
    @Nullable
    private final InteractionSourceTracker sourceTracker;
    private final ContainerCacheMode cacheMode;
    @Nullable
    private File dataDir;
    private boolean savedForUnload;

    public GuiLifecycleBridge(GuiSessionManager manager) {
        this(manager, null, ContainerCacheMode.HYBRID, null);
    }

    public GuiLifecycleBridge(GuiSessionManager manager, @Nullable InteractionSourceTracker sourceTracker, ContainerCacheMode cacheMode) {
        this(manager, sourceTracker, cacheMode, null);
    }

    public GuiLifecycleBridge(GuiSessionManager manager, @Nullable InteractionSourceTracker sourceTracker,
                              ContainerCacheMode cacheMode, @Nullable File dataDir) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.sourceTracker = sourceTracker;
        this.cacheMode = Objects.requireNonNull(cacheMode, "cacheMode");
        this.dataDir = dataDir;
    }

    public void setDataDir(@Nullable File dataDir) {
        this.dataDir = dataDir;
    }

    public TransitionDecision onBeforeDisplay(@Nullable GuiScreen current, @Nullable GuiScreen incoming, boolean explicitDestroy) {
        if (current != null && current == incoming) {
            GuiSession currentSession = manager.findSessionByScreen(current);
            GuiSessionId currentSessionId = currentSession == null ? null : currentSession.getId();
            return new TransitionDecision(false, null, currentSessionId);
        }

        long now = System.currentTimeMillis();
        boolean hasSource = sourceTracker != null && sourceTracker.hasPending(now);
        TrackingDecision incomingDecision = GuiTrackingPolicy.decide(incoming, hasSource);

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
                    hiddenSessionId = currentSession.getId();
                    suppressCurrentClose = cacheMode == ContainerCacheMode.HYBRID;
                }
            }
        }

        // Handle incoming screen
        GuiSessionId activatedSessionId = null;
        if (incoming != null) {
            GuiSession existingIncomingSession = manager.findSessionByScreen(incoming);
            if (existingIncomingSession != null) {
                manager.activateSession(existingIncomingSession.getId());
                activatedSessionId = existingIncomingSession.getId();
                return new TransitionDecision(suppressCurrentClose, hiddenSessionId, activatedSessionId);
            }
            if (incomingDecision == TrackingDecision.TRACK_AS_TAB) {
                GuiSessionSource source = null;
                if (sourceTracker != null) {
                    source = sourceTracker.consumePending(now);
                }

                GuiSession incomingSession = manager.registerOrReuseSession(incoming, null, source);
                activatedSessionId = incomingSession.getId();
                if (incoming instanceof GuiContainer) {
                    manager.setLastServerWindowSessionId(incomingSession.getId());
                }
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

        long now = System.currentTimeMillis();
        boolean hasSource = sourceTracker != null && sourceTracker.hasPending(now);
        TrackingDecision decision = GuiTrackingPolicy.decide(nowVisible, hasSource);

        if (decision == TrackingDecision.TRACK_AS_TAB) {
            GuiSessionSource source = null;
            if (sourceTracker != null) {
                source = sourceTracker.consumePending(now);
            }
            GuiSession registered = manager.registerOrReuseSession(nowVisible, null, source);
            if (nowVisible instanceof GuiContainer) {
                manager.setLastServerWindowSessionId(registered.getId());
            }
        }
    }

    public void onWorldUnload() {
        if (dataDir != null && !savedForUnload) {
            savedForUnload = true;
            TabPersistenceManager.save(manager.listAllSessions(), dataDir);
            if (manager.getHistoryStore() != null) {
                manager.getHistoryStore().save(dataDir);
            }
            if (manager.getBookmarkStore() != null) {
                manager.getBookmarkStore().save(dataDir);
            }
        }
        manager.clearForWorldUnload();
        if (sourceTracker != null) {
            sourceTracker.clear();
        }
    }

    public void resetSaveGuard() {
        savedForUnload = false;
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
