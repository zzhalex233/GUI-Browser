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
import com.zzhalex233.guibrowser.client.session.StaleTabPlaceholderScreen;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

public final class GuiChromeOverlayController {

    public enum TabSwitchResult { DIRECT_SWITCH, RESTORE_INITIATED, FAILED }

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

    public TabSwitchResult handleTabLeftClick(GuiSessionId sessionId) {
        GuiSession session = sessionManager.findSession(sessionId);
        if (session == null) {
            return TabSwitchResult.FAILED;
        }

        if (session.getScreen() instanceof StaleTabPlaceholderScreen) {
            return restorePlaceholderSession(session);
        }

        sessionManager.activateSession(sessionId);
        if (!(session.getScreen() instanceof net.minecraft.client.gui.inventory.GuiContainer)) {
            return TabSwitchResult.DIRECT_SWITCH;
        }

        if (restoreHandler != null
                && session.getSource() != null
                && !sessionId.equals(sessionManager.getLastServerWindowSessionId())) {
            restoreHandler.requestSync(session);
        }
        return TabSwitchResult.DIRECT_SWITCH;
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

    private TabSwitchResult restorePlaceholderSession(GuiSession session) {
        if (restoreHandler == null || session.getSource() == null) {
            showRestoreFailedToast(session);
            return TabSwitchResult.FAILED;
        }

        boolean restored = restoreHandler.requestRestore(session);
        if (!restored) {
            showRestoreFailedToast(session);
            return TabSwitchResult.FAILED;
        }
        return TabSwitchResult.RESTORE_INITIATED;
    }

    private void showRestoreFailedToast(GuiSession session) {
        GuiSessionSource source = session.getSource();
        String msg;
        if (source instanceof GuiSessionSource.BlockSource) {
            GuiSessionSource.BlockSource blockSource = (GuiSessionSource.BlockSource) source;
            msg = "Cannot reach " + session.getTitle() + " at "
                + blockSource.getPos().getX() + ", " + blockSource.getPos().getY() + ", " + blockSource.getPos().getZ();
        } else {
            msg = "Cannot restore " + session.getTitle();
        }
        GuiRestoreFailedToast.show(msg);
    }
}
