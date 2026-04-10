package com.zzhalex233.guibrowser.client.browser;

import com.zzhalex233.guibrowser.config.BrowserConfig;

import java.util.Objects;

public final class BrowserManager {
    private static BrowserManager instance;

    private final BrowserConfig config;
    private final BrowserWindowState windowState;
    private BrowserState state;
    private Object hostedContent;
    private String activeTabTitle;
    private Object lastMinimizedHostedContent;
    private String lastMinimizedTabTitle;

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

    public Object getHostedContent() {
        return hostedContent;
    }

    public boolean hasHostedContent() {
        return hostedContent != null;
    }

    public String getActiveTabTitle() {
        if (activeTabTitle == null || activeTabTitle.trim().isEmpty()) {
            return "GUI";
        }
        return activeTabTitle;
    }

    public void openEmptyBrowser() {
        hostedContent = null;
        activeTabTitle = null;
        state = BrowserState.OPEN_EMPTY;
    }

    public void captureHostedContent(Object hostedContent, String tabTitle) {
        this.hostedContent = Objects.requireNonNull(hostedContent, "hostedContent");
        this.activeTabTitle = sanitizeTabTitle(tabTitle);
        this.state = BrowserState.OPEN_WITH_TABS;
        this.windowState.endDrag();
    }

    public void minimizeBrowser() {
        if (state == BrowserState.OPEN_WITH_TABS && hostedContent != null) {
            lastMinimizedHostedContent = hostedContent;
            lastMinimizedTabTitle = activeTabTitle;
            state = BrowserState.MINIMIZED_WITH_TABS;
        } else if (state == BrowserState.OPEN_EMPTY) {
            state = BrowserState.CLOSED;
        }
        windowState.endDrag();
    }

    public void restoreBrowser() {
        if (state == BrowserState.MINIMIZED_WITH_TABS && lastMinimizedHostedContent != null) {
            hostedContent = lastMinimizedHostedContent;
            activeTabTitle = lastMinimizedTabTitle;
            state = BrowserState.OPEN_WITH_TABS;
        }
    }

    public void clearHostedContent() {
        hostedContent = null;
        activeTabTitle = null;
        if (state != BrowserState.CLOSED) {
            state = BrowserState.OPEN_EMPTY;
        }
        windowState.endDrag();
    }

    public void closeBrowser() {
        hostedContent = null;
        activeTabTitle = null;
        lastMinimizedHostedContent = null;
        lastMinimizedTabTitle = null;
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

    private static String sanitizeTabTitle(String tabTitle) {
        if (tabTitle == null) {
            return "GUI";
        }
        String trimmed = tabTitle.trim();
        return trimmed.isEmpty() ? "GUI" : trimmed;
    }
}