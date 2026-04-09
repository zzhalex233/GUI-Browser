package com.zzhalex233.guibrowser.client.browser;

import com.zzhalex233.guibrowser.client.gui.BrowserRootGui;
import net.minecraft.client.gui.GuiScreen;

import java.util.Objects;

public class BrowserShellController {
    private final BrowserManager manager;
    private final BrowserShellHost host;

    public BrowserShellController(BrowserManager manager, BrowserShellHost host) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.host = Objects.requireNonNull(host, "host");
    }

    public BrowserManager getManager() {
        return manager;
    }

    public boolean openBrowser() {
        if (!host.isInWorld()) {
            return false;
        }
        if (manager.getState() == BrowserState.CLOSED) {
            manager.openEmptyBrowser();
        }
        host.showBrowserRoot(this);
        return true;
    }

    public boolean closeBrowser() {
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
        return closeBrowser();
    }

    public boolean handleChromeCloseOrMinimize() {
        return closeBrowser();
    }

    public boolean handleExternalGuiOpen(GuiScreen incomingGui) {
        if (manager.getState() == BrowserState.CLOSED || incomingGui instanceof BrowserRootGui) {
            return false;
        }
        manager.closeBrowser();
        return true;
    }
}
