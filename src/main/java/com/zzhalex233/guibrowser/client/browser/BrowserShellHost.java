package com.zzhalex233.guibrowser.client.browser;

public interface BrowserShellHost {
    boolean isInWorld();

    boolean isBrowserRootActive();

    boolean canTriggerQuickCapture();

    void showBrowserRoot(BrowserShellController controller);

    void prepareToCloseBrowser(BrowserShellController controller);

    void closeCurrentScreen();

    void performQuickCaptureInteraction(BrowserShellController controller);
}