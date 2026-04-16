package com.zzhalex233.guibrowser.client.chrome;

import com.zzhalex233.guibrowser.client.history.GuiBookmarkStore;
import com.zzhalex233.guibrowser.client.history.GuiHistoryStore;
import com.zzhalex233.guibrowser.client.popup.GuiBookmarkEditDialog;
import com.zzhalex233.guibrowser.client.popup.GuiBookmarkPanel;
import com.zzhalex233.guibrowser.client.popup.GuiHistoryPanel;
import com.zzhalex233.guibrowser.client.popup.GuiRestoreFailedToast;
import com.zzhalex233.guibrowser.client.session.ContainerRestoreHandler;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionId;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

public final class GuiChromeOverlayController {

    private final GuiSessionManager sessionManager;
    @Nullable
    private final ContainerRestoreHandler restoreHandler;
    @Nullable
    private File dataDir;
    private boolean historyPanelOpen;
    private boolean bookmarkPanelOpen;

    public GuiChromeOverlayController(GuiSessionManager sessionManager) {
        this(sessionManager, null);
    }

    public GuiChromeOverlayController(GuiSessionManager sessionManager, @Nullable ContainerRestoreHandler restoreHandler) {
        this.sessionManager = sessionManager;
        this.restoreHandler = restoreHandler;
    }

    public void setDataDir(@Nullable File dataDir) {
        this.dataDir = dataDir;
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
                showRestoreFailedToast(session);
            }
            return;
        }
        sessionManager.activateSession(sessionId);
    }

    public void handleTabMiddleClick(GuiSessionId sessionId) {
        sessionManager.destroySession(sessionId);
    }

    public void handleBookmarkButtonClick(net.minecraft.client.gui.GuiScreen currentScreen) {
        GuiSession foreground = sessionManager.getForegroundSession();
        if (foreground == null) return;
        GuiBookmarkStore store = sessionManager.getBookmarkStore();
        if (store == null) return;
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
            new GuiBookmarkEditDialog(currentScreen, sessionManager, store, foreground, dataDir));
    }

    public void handleBookmarkButtonRightClick(net.minecraft.client.gui.GuiScreen currentScreen) {
        GuiBookmarkStore store = sessionManager.getBookmarkStore();
        if (store == null) return;
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
            new GuiBookmarkPanel(currentScreen, store, sessionManager, restoreHandler, dataDir));
    }

    public void handleHistoryButtonClick(net.minecraft.client.gui.GuiScreen currentScreen) {
        GuiHistoryStore store = sessionManager.getHistoryStore();
        if (store == null) {
            historyPanelOpen = !historyPanelOpen;
            bookmarkPanelOpen = false;
            return;
        }
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
            new GuiHistoryPanel(currentScreen, store, sessionManager, restoreHandler, dataDir));
    }

    public boolean isHistoryPanelOpen() {
        return historyPanelOpen;
    }

    public boolean isBookmarkPanelOpen() {
        return bookmarkPanelOpen;
    }

    public boolean isSessionBookmarked(GuiSessionId sessionId) {
        return sessionManager.isSessionBookmarked(sessionId);
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

    private void showRestoreFailedToast(GuiSession session) {
        GuiSessionSource source = session.getSource();
        String msg;
        if (source instanceof GuiSessionSource.BlockSource) {
            GuiSessionSource.BlockSource bs = (GuiSessionSource.BlockSource) source;
            msg = "Cannot reach " + session.getTitle() + " at "
                + bs.getPos().getX() + ", " + bs.getPos().getY() + ", " + bs.getPos().getZ();
        } else {
            msg = "Cannot restore " + session.getTitle();
        }
        GuiRestoreFailedToast.show(msg);
    }
}
