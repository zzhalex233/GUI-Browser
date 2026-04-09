package com.zzhalex233.guibrowser.client.browser;

import com.zzhalex233.guibrowser.config.BrowserConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserShellControllerTest {

    @Test
    void toggleFromClosedOpensEmptyBrowserAndRequestsRootScreen() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        FakeHost host = new FakeHost(true, false);
        BrowserShellController controller = new BrowserShellController(manager, host);

        controller.toggleBrowser();

        assertEquals(BrowserState.OPEN_EMPTY, manager.getState());
        assertEquals(1, host.openBrowserRootRequests);
        assertEquals(0, host.closeCurrentScreenRequests);
        assertTrue(host.browserRootActive);
    }

    @Test
    void toggleWhileBrowserRootActiveClosesBrowserAndRequestsCurrentScreenClose() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.openEmptyBrowser();
        FakeHost host = new FakeHost(true, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        controller.toggleBrowser();

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertEquals(0, host.openBrowserRootRequests);
        assertEquals(1, host.closeCurrentScreenRequests);
        assertFalse(host.browserRootActive);
    }

    @Test
    void escFromRootClosesBrowserAndEndsClosed() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.openEmptyBrowser();
        FakeHost host = new FakeHost(true, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        controller.handleEscFromRoot();

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertEquals(1, host.closeCurrentScreenRequests);
        assertFalse(host.browserRootActive);
    }

    @Test
    void chromeCloseOrMinimizeClosesEmptyShell() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.openEmptyBrowser();
        FakeHost host = new FakeHost(true, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        controller.handleChromeCloseOrMinimize();

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertEquals(1, host.closeCurrentScreenRequests);
        assertFalse(host.browserRootActive);
    }

    private static final class FakeHost implements BrowserShellHost {
        private final boolean inWorld;
        private boolean browserRootActive;
        private int openBrowserRootRequests;
        private int closeCurrentScreenRequests;

        private FakeHost(boolean inWorld, boolean browserRootActive) {
            this.inWorld = inWorld;
            this.browserRootActive = browserRootActive;
        }

        @Override
        public boolean isInWorld() {
            return inWorld;
        }

        @Override
        public boolean isBrowserRootActive() {
            return browserRootActive;
        }

        @Override
        public void showBrowserRoot(BrowserShellController controller) {
            openBrowserRootRequests++;
            browserRootActive = true;
        }

        @Override
        public void closeCurrentScreen() {
            closeCurrentScreenRequests++;
            browserRootActive = false;
        }
    }
}
