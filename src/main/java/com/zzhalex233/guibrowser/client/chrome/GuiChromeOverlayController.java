package com.zzhalex233.guibrowser.client.chrome;

import com.zzhalex233.guibrowser.client.history.GuiBookmarkStore;
import com.zzhalex233.guibrowser.client.session.ContainerRestoreHandler;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionId;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;

import javax.annotation.Nullable;
import java.util.List;

public final class GuiChromeOverlayController {

    private final GuiSessionManager sessionManager;
    @Nullable
    private final ContainerRestoreHandler restoreHandler;
    private boolean historyPanelOpen;
    private boolean bookmarkPanelOpen;

    public GuiChromeOverlayController(GuiSessionManager sessionManager) {
        this(sessionManager, null);
    }

    public GuiChromeOverlayController(GuiSessionManager sessionManager, @Nullable ContainerRestoreHandler restoreHandler) {
        this.sessionManager = sessionManager;
        this.restoreHandler = restoreHandler;
    }

    public List<GuiSession> getTabs() {
        return sessionManager.listAllSessions();
    }

    public GuiSession getForegroundSession() {
        return sessionManager.getForegroundSession();
    }

    public void handleTabLeftClick(GuiSessionId sessionId) {
        GuiSession session = sessionManager.findSession(sessionId);
        if (session == null) {
            return;
        }
        if (session.isStale() && restoreHandler != null) {
            boolean restored = restoreHandler.requestRestore(session);
            if (!restored) {
                sessionManager.destroySession(sessionId);
            }
            return;
        }
        sessionManager.activateSession(sessionId);
    }

    public void handleTabMiddleClick(GuiSessionId sessionId) {
        sessionManager.destroySession(sessionId);
    }

    public void handleBookmarkButtonClick() {
        GuiSession foreground = sessionManager.getForegroundSession();
        if (foreground != null) {
            sessionManager.toggleBookmark(foreground.getId());
        }
    }

    public void handleHistoryButtonClick() {
        historyPanelOpen = !historyPanelOpen;
        bookmarkPanelOpen = false;
    }

    public boolean isHistoryPanelOpen() {
        return historyPanelOpen;
    }

    public boolean isBookmarkPanelOpen() {
        return bookmarkPanelOpen;
    }

    public boolean isSessionBookmarked(GuiSessionId sessionId) {
        GuiBookmarkStore store = sessionManager.getBookmarkStore();
        return store != null && store.isBookmarked(sessionId);
    }

    public GuiChromeTarget resolveTarget(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        GuiChromeLayout layout = new GuiChromeLayout(screenWidth, screenHeight);
        List<GuiSession> tabs = getTabs();
        return layout.hitTest(mouseX, mouseY, tabs.size());
    }

    public boolean isInsideTopBar(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        GuiChromeLayout layout = new GuiChromeLayout(screenWidth, screenHeight);
        return layout.topBarRect().contains(mouseX, mouseY);
    }

    public GuiSessionId getSessionIdForTabIndex(int tabIndex) {
        List<GuiSession> tabs = getTabs();
        if (tabIndex < 0 || tabIndex >= tabs.size()) {
            return null;
        }
        return tabs.get(tabIndex).getId();
    }
}
