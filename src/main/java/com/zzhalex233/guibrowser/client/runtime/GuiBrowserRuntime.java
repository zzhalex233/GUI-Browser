package com.zzhalex233.guibrowser.client.runtime;

import com.zzhalex233.guibrowser.client.chrome.GuiChromeOverlayController;
import com.zzhalex233.guibrowser.client.chrome.GuiChromeRenderer;
import com.zzhalex233.guibrowser.client.history.GuiBookmarkStore;
import com.zzhalex233.guibrowser.client.history.GuiHistoryStore;
import com.zzhalex233.guibrowser.client.persistence.JsonPersistence;
import com.zzhalex233.guibrowser.client.session.ContainerRestoreHandler;
import com.zzhalex233.guibrowser.client.session.GuiLifecycleBridge;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.InteractionSourceTracker;
import com.zzhalex233.guibrowser.config.BrowserConfig;

import java.io.File;
import java.util.Objects;

public final class GuiBrowserRuntime {
    private static GuiBrowserRuntime instance;

    private final BrowserConfig config;
    private final File gameDir;
    private File dataDir;
    private final GuiHistoryStore historyStore;
    private final GuiBookmarkStore bookmarkStore;
    private final GuiSessionManager sessionManager;
    private final GuiLifecycleBridge lifecycleBridge;
    private final GuiChromeOverlayController chromeController;
    private final GuiChromeRenderer chromeRenderer;
    private final InteractionSourceTracker sourceTracker;
    private final ContainerRestoreHandler restoreHandler;

    private boolean suppressClosePacket;
    private volatile boolean bypassServerDistanceCheck;
    private volatile boolean keepContainerOpen;

    private GuiBrowserRuntime(BrowserConfig config) {
        this(config, null);
    }

    private GuiBrowserRuntime(BrowserConfig config, File gameDir) {
        this.config = Objects.requireNonNull(config, "config");
        this.gameDir = gameDir;
        this.dataDir = null;
        this.historyStore = config.isEnableHistoryPanel() ? new GuiHistoryStore() : null;
        this.bookmarkStore = config.isEnableBookmarks() ? new GuiBookmarkStore() : null;
        this.sessionManager = new GuiSessionManager(historyStore, bookmarkStore);
        this.sourceTracker = new InteractionSourceTracker();
        ContainerRestoreHandler restoreHandler = new ContainerRestoreHandler(
            config.getContainerCacheMode(), sourceTracker, sessionManager);
        this.restoreHandler = restoreHandler;
        this.lifecycleBridge = new GuiLifecycleBridge(sessionManager, sourceTracker, config.getContainerCacheMode());
        this.chromeController = new GuiChromeOverlayController(sessionManager, restoreHandler);
        this.chromeRenderer = new GuiChromeRenderer(chromeController);
    }

    public void updateDataDirForWorld(String worldId) {
        if (gameDir == null) return;
        this.dataDir = JsonPersistence.getWorldDataDir(gameDir, worldId);
        this.lifecycleBridge.setDataDir(dataDir);
        this.lifecycleBridge.resetSaveGuard();
        this.chromeController.setDataDir(dataDir);
        if (historyStore != null) historyStore.load(dataDir);
        if (bookmarkStore != null) bookmarkStore.load(dataDir);
    }

    public static GuiBrowserRuntime initialize(BrowserConfig config) {
        instance = new GuiBrowserRuntime(config);
        return instance;
    }

    public static GuiBrowserRuntime initialize(BrowserConfig config, File gameDir) {
        instance = new GuiBrowserRuntime(config, gameDir);
        return instance;
    }

    public static GuiBrowserRuntime getInstance() {
        if (instance == null) {
            instance = new GuiBrowserRuntime(BrowserConfig.defaults());
        }
        return instance;
    }

    public static void resetForTests() {
        instance = null;
    }

    public BrowserConfig getConfig() {
        return config;
    }

    public File getDataDir() {
        return dataDir;
    }

    public GuiSessionManager getSessionManager() {
        return sessionManager;
    }

    public GuiLifecycleBridge getLifecycleBridge() {
        return lifecycleBridge;
    }

    public GuiChromeOverlayController getChromeController() {
        return chromeController;
    }

    public GuiChromeRenderer getChromeRenderer() {
        return chromeRenderer;
    }

    public boolean isSuppressClosePacket() {
        return suppressClosePacket;
    }

    public void setSuppressClosePacket(boolean value) {
        this.suppressClosePacket = value;
    }

    public boolean isBypassServerDistanceCheck() {
        return bypassServerDistanceCheck;
    }

    public void setBypassServerDistanceCheck(boolean value) {
        this.bypassServerDistanceCheck = value;
    }

    public boolean isKeepContainerOpen() {
        return keepContainerOpen;
    }

    public void setKeepContainerOpen(boolean value) {
        this.keepContainerOpen = value;
    }

    public InteractionSourceTracker getSourceTracker() {
        return sourceTracker;
    }

    public ContainerRestoreHandler getRestoreHandler() {
        return restoreHandler;
    }
}
