package com.zzhalex233.guibrowser.client.browser;

import com.zzhalex233.guibrowser.config.BrowserConfig;

import java.util.Objects;

public final class BrowserManager {
    private static BrowserManager instance;

    private final BrowserConfig config;
    private final BrowserWindowState windowState;
    private BrowserState state;

    private BrowserManager(BrowserConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.windowState = BrowserWindowState.defaultWindow();
        this.state = BrowserState.CLOSED;
    }

    public static BrowserManager initialize(BrowserConfig config) {
        instance = new BrowserManager(config);
        return instance;
    }

    public static BrowserManager getInstance() {
        if (instance == null) {
            instance = new BrowserManager(BrowserConfig.defaults());
        }
        return instance;
    }

    public static BrowserManager createForTests(BrowserConfig config) {
        return new BrowserManager(config);
    }

    public BrowserState getState() {
        return state;
    }

    public BrowserWindowState getWindowState() {
        return windowState;
    }

    public BrowserConfig getConfig() {
        return config;
    }

    public void openEmptyBrowser() {
        state = BrowserState.OPEN_EMPTY;
    }

    public void closeBrowser() {
        state = BrowserState.CLOSED;
        windowState.endDrag();
    }

    public void toggleBrowser() {
        if (state == BrowserState.CLOSED) {
            openEmptyBrowser();
        } else {
            closeBrowser();
        }
    }

    public void handleEscFromRoot() {
        if (state != BrowserState.CLOSED) {
            closeBrowser();
        }
    }
}
