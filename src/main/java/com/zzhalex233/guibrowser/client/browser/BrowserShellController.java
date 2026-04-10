package com.zzhalex233.guibrowser.client.browser;

import java.util.Objects;

public class BrowserShellController {
    private final BrowserManager manager;
    private final BrowserShellHost host;
    private boolean delegatingToHostedContent;
    private boolean pendingShowRoot;

    public BrowserShellController(BrowserManager manager, BrowserShellHost host) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.host = Objects.requireNonNull(host, "host");
    }

    public BrowserManager getManager() {
        return manager;
    }

    public Object getHostedContent() {
        return manager.getHostedContent();
    }

    public boolean hasHostedContent() {
        return manager.hasHostedContent();
    }

    public String getActiveTabTitle() {
        return manager.getActiveTabTitle();
    }

    public boolean isInWorld() {
        return host.isInWorld();
    }

    public boolean isBrowserRootActive() {
        return host.isBrowserRootActive();
    }

    public boolean isDelegatingToHostedContent() {
        return delegatingToHostedContent;
    }

    public boolean isCaptureRequested() {
        return manager.hasPendingCaptureRequest();
    }

    public void beginHostedContentDelegation() {
        delegatingToHostedContent = true;
    }

    public void endHostedContentDelegation() {
        delegatingToHostedContent = false;
    }

    public void captureHostedContent(Object hostedContent, String title) {
        manager.captureHostedContent(hostedContent, title);
    }

    public void clearHostedContent() {
        manager.clearHostedContent();
    }

    public boolean requestBrowserOpenFromCommand() {
        if (!host.isInWorld()) {
            return false;
        }
        openOrRestoreBrowserState();
        if (!host.isBrowserRootActive()) {
            pendingShowRoot = true;
        }
        return true;
    }

    public boolean requestHotkeyCapture() {
        return requestQuickCapture();
    }

    public boolean requestQuickCapture() {
        if (!host.canTriggerQuickCapture()) {
            return false;
        }
        manager.armCaptureRequest(BrowserCaptureRequest.hotkeyRequest());
        try {
            host.performQuickCaptureInteraction(this);
        } finally {
            manager.clearCaptureRequest();
        }
        return true;
    }

    public void tickCaptureRequest() {
        manager.tickCaptureRequest();
    }

    public boolean hasPendingCaptureRequest() {
        return manager.hasPendingCaptureRequest();
    }

    public BrowserCaptureRequest consumeCaptureRequest() {
        return manager.consumeCaptureRequest();
    }

    public void clearCaptureRequest() {
        manager.clearCaptureRequest();
    }

    public void flushDeferredUiActions() {
        if (!pendingShowRoot) {
            return;
        }
        host.showBrowserRoot(this);
        pendingShowRoot = false;
    }

    public boolean openBrowser() {
        if (!host.isInWorld()) {
            return false;
        }
        openOrRestoreBrowserState();
        if (!host.isBrowserRootActive()) {
            host.showBrowserRoot(this);
        }
        pendingShowRoot = false;
        return true;
    }

    public boolean closeBrowser() {
        pendingShowRoot = false;
        host.prepareToCloseBrowser(this);
        manager.closeBrowser();
        if (host.isBrowserRootActive()) {
            host.closeCurrentScreen();
        }
        return true;
    }

    public boolean toggleBrowser() {
        if (!host.isInWorld()) {
            return false;
        }
        if (manager.getState() == BrowserState.CLOSED) {
            return openBrowser();
        }
        if (host.isBrowserRootActive()) {
            return closeBrowser();
        }
        return openBrowser();
    }

    public boolean handleEscFromRoot() {
        BrowserState previousState = manager.getState();
        manager.handleEscFromRoot();
        if ((manager.getState() == BrowserState.CLOSED || manager.getState() == BrowserState.MINIMIZED_WITH_TABS)
            && host.isBrowserRootActive()) {
            if (manager.getState() == BrowserState.CLOSED) {
                host.prepareToCloseBrowser(this);
            }
            host.closeCurrentScreen();
        }
        return previousState != manager.getState();
    }

    public boolean handleChromeCloseOrMinimize() {
        return closeBrowser();
    }

    public void handleExternalCloseSignal() {
        if (pendingShowRoot || host.isBrowserRootActive()) {
            return;
        }
        if (manager.getState() != BrowserState.CLOSED) {
            manager.closeBrowser();
        }
    }

    private void openOrRestoreBrowserState() {
        if (manager.getState() == BrowserState.MINIMIZED_WITH_TABS) {
            manager.restoreBrowser();
        } else if (manager.getState() == BrowserState.CLOSED) {
            manager.openEmptyBrowser();
        }
    }
}