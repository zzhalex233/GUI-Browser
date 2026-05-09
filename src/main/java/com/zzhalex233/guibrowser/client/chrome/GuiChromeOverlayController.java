package com.zzhalex233.guibrowser.client.chrome;

import com.zzhalex233.guibrowser.client.history.GuiBookmarkStore;
import com.zzhalex233.guibrowser.client.history.GuiHistoryStore;
import com.zzhalex233.guibrowser.client.popup.GuiBookmarkEditDialog;
import com.zzhalex233.guibrowser.client.popup.GuiBookmarkPanel;
import com.zzhalex233.guibrowser.client.popup.GuiHistoryPanel;
import com.zzhalex233.guibrowser.client.popup.GuiRestoreFailedToast;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionId;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiRestoreRequester;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

public final class GuiChromeOverlayController {

    public enum TabSwitchResult { DIRECT_SWITCH, RESTORE_INITIATED, FAILED }

    private final GuiSessionManager sessionManager;
    @Nullable
    private final GuiRestoreRequester restoreHandler;
    @Nullable
    private File dataDir;
    private int tabScrollOffset;

    public GuiChromeOverlayController(GuiSessionManager sessionManager) {
        this(sessionManager, null);
    }

    public GuiChromeOverlayController(GuiSessionManager sessionManager, @Nullable GuiRestoreRequester restoreHandler) {
        this.sessionManager = sessionManager;
        this.restoreHandler = restoreHandler;
    }

    public void setDataDir(@Nullable File dataDir) {
        this.dataDir = dataDir;
    }

    public List<GuiSession> getTabs() {
        return sessionManager.listAllSessionsForCurrentDimension();
    }

    public GuiSession getForegroundSession() {
        return sessionManager.getForegroundSession();
    }

    public TabSwitchResult handleTabLeftClick(GuiSessionId sessionId) {
        GuiSession session = sessionManager.findSession(sessionId);
        if (session == null) {
            return TabSwitchResult.FAILED;
        }

        if (!(session.getScreen() instanceof net.minecraft.client.gui.inventory.GuiContainer)) {
            sessionManager.activateSession(sessionId);
            return TabSwitchResult.DIRECT_SWITCH;
        }

        if (sessionId.equals(sessionManager.getLastServerWindowSessionId())) {
            sessionManager.activateSession(sessionId);
            return TabSwitchResult.DIRECT_SWITCH;
        }

        if (restoreHandler == null || session.getSource() == null) {
            showRestoreFailedToast(session);
            return TabSwitchResult.FAILED;
        }

        boolean restored = restoreHandler.requestSync(session);
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
            msg = I18n.format("guibrowser.restore.unreachable", session.getTitle(),
                blockSource.getPos().getX(), blockSource.getPos().getY(), blockSource.getPos().getZ());
        } else {
            msg = I18n.format("guibrowser.restore.failed", session.getTitle());
        }
        GuiRestoreFailedToast.show(msg);
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
            return;
        }
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
            new GuiHistoryPanel(currentScreen, store, sessionManager, restoreHandler, dataDir));
    }

    public boolean isSessionBookmarked(GuiSessionId sessionId) {
        return sessionManager.isSessionBookmarked(sessionId);
    }

    public GuiChromeTarget resolveTarget(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        GuiChromeLayout layout = createLayout(screenWidth, screenHeight);
        List<GuiSession> tabs = getTabs();
        return layout.hitTest(mouseX, mouseY, tabs.size());
    }

    public boolean isInsideTopBar(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        GuiChromeLayout layout = createLayout(screenWidth, screenHeight);
        return layout.topBarRect().contains(mouseX, mouseY);
    }

    public GuiChromeLayout createLayout(int screenWidth, int screenHeight) {
        GuiChromeLayout layout = new GuiChromeLayout(screenWidth, screenHeight, tabScrollOffset);
        int max = layout.maxScrollOffset(getTabs().size());
        if (tabScrollOffset > max) {
            tabScrollOffset = max;
            layout = new GuiChromeLayout(screenWidth, screenHeight, tabScrollOffset);
        }
        return layout;
    }

    public boolean scrollTabs(int wheelDelta, int screenWidth, int screenHeight) {
        GuiChromeLayout layout = createLayout(screenWidth, screenHeight);
        int max = layout.maxScrollOffset(getTabs().size());
        if (max <= 0 || wheelDelta == 0) {
            return false;
        }
        int direction = wheelDelta > 0 ? -1 : 1;
        int next = tabScrollOffset + direction * (GuiChromeLayout.TAB_WIDTH / 2);
        next = Math.max(0, Math.min(max, next));
        if (next == tabScrollOffset) {
            return false;
        }
        tabScrollOffset = next;
        return true;
    }

    public GuiSessionId getSessionIdForTabIndex(int tabIndex) {
        List<GuiSession> tabs = getTabs();
        if (tabIndex < 0 || tabIndex >= tabs.size()) {
            return null;
        }
        return tabs.get(tabIndex).getId();
    }

}
