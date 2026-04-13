package com.zzhalex233.guibrowser.client.runtime;

import com.zzhalex233.guibrowser.client.chrome.GuiChromeOverlayController;
import com.zzhalex233.guibrowser.client.chrome.GuiChromeRenderer;
import com.zzhalex233.guibrowser.client.history.GuiBookmarkStore;
import com.zzhalex233.guibrowser.client.history.GuiHistoryStore;
import com.zzhalex233.guibrowser.client.session.ContainerRestoreHandler;
import com.zzhalex233.guibrowser.client.session.GuiLifecycleBridge;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.config.BrowserConfig;

import javax.annotation.Nullable;

import java.util.Objects;

public final class GuiBrowserRuntime {
    private static GuiBrowserRuntime instance;

    private final BrowserConfig config;
    private final GuiHistoryStore historyStore;
    private final GuiBookmarkStore bookmarkStore;
    private final GuiSessionManager sessionManager;
    private final GuiLifecycleBridge lifecycleBridge;
    private final GuiChromeOverlayController chromeController;
    private final GuiChromeRenderer chromeRenderer;

    @Nullable
    private ContainerRestoreHandler restoreHandler;
    private boolean suppressClosePacket;

    private GuiBrowserRuntime(BrowserConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.historyStore = config.isEnableHistoryPanel() ? new GuiHistoryStore() : null;
        this.bookmarkStore = config.isEnableBookmarks() ? new GuiBookmarkStore() : null;
        this.sessionManager = new GuiSessionManager(historyStore, bookmarkStore);
        this.lifecycleBridge = new GuiLifecycleBridge(sessionManager);
        this.chromeController = new GuiChromeOverlayController(sessionManager);
        this.chromeRenderer = new GuiChromeRenderer(chromeController);
    }

    public static GuiBrowserRuntime initialize(BrowserConfig config) {
        instance = new GuiBrowserRuntime(config);
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

    @Nullable
    public ContainerRestoreHandler getRestoreHandler() {
        return restoreHandler;
    }

    public void setRestoreHandler(@Nullable ContainerRestoreHandler restoreHandler) {
        this.restoreHandler = restoreHandler;
    }
}
