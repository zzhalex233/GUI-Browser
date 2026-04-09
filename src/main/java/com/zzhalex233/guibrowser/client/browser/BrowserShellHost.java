package com.zzhalex233.guibrowser.client.browser;

public interface BrowserShellHost {
    boolean isInWorld();

    boolean isBrowserRootActive();

    void showBrowserRoot(BrowserShellController controller);

    void closeCurrentScreen();
}
