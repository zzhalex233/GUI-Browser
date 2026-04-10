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
        FakeHost host = new FakeHost(true, false, true);
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
        FakeHost host = new FakeHost(true, true, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        controller.toggleBrowser();

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertEquals(0, host.openBrowserRootRequests);
        assertEquals(1, host.prepareToCloseBrowserRequests);
        assertEquals(1, host.closeCurrentScreenRequests);
        assertFalse(host.browserRootActive);
    }

    @Test
    void escFromRootMinimizesHostedBrowserAndClosesRootScreen() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.captureHostedContent(new Object(), "Chest");
        FakeHost host = new FakeHost(true, true, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        controller.handleEscFromRoot();

        assertEquals(BrowserState.MINIMIZED_WITH_TABS, manager.getState());
        assertEquals(0, host.prepareToCloseBrowserRequests);
        assertEquals(1, host.closeCurrentScreenRequests);
        assertFalse(host.browserRootActive);
    }

    @Test
    void chromeCloseOrMinimizeClosesEmptyShell() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.openEmptyBrowser();
        FakeHost host = new FakeHost(true, true, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        controller.handleChromeCloseOrMinimize();

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertEquals(1, host.prepareToCloseBrowserRequests);
        assertEquals(1, host.closeCurrentScreenRequests);
        assertFalse(host.browserRootActive);
    }

    @Test
    void commandOpenQueuesBrowserRootUntilFlush() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        FakeHost host = new FakeHost(true, false, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        assertTrue(controller.requestBrowserOpenFromCommand());
        assertEquals(BrowserState.OPEN_EMPTY, manager.getState());
        assertEquals(0, host.openBrowserRootRequests);
        assertFalse(host.browserRootActive);

        controller.flushDeferredUiActions();

        assertEquals(1, host.openBrowserRootRequests);
        assertTrue(host.browserRootActive);
    }

    @Test
    void commandOpenRestoresMinimizedBrowserInsteadOfOpeningEmpty() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        FakeHost host = new FakeHost(true, false, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        manager.captureHostedContent(new Object(), "Chest");
        manager.minimizeBrowser();

        assertTrue(controller.requestBrowserOpenFromCommand());
        controller.flushDeferredUiActions();

        assertEquals(BrowserState.OPEN_WITH_TABS, manager.getState());
        assertTrue(manager.hasHostedContent());
        assertEquals(1, host.openBrowserRootRequests);
        assertTrue(host.browserRootActive);
    }

    @Test
    void hotkeyCaptureTriggersOriginalInteractionWithoutLeavingPendingRequest() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        FakeHost host = new FakeHost(true, false, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        assertTrue(controller.requestHotkeyCapture());

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertFalse(controller.hasPendingCaptureRequest());
        assertEquals(1, host.quickCaptureInteractionRequests);
        assertEquals(0, host.openBrowserRootRequests);
        assertFalse(host.browserRootActive);
    }

    @Test
    void hotkeyCaptureReturnsFalseWhenQuickCaptureCannotRun() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        FakeHost host = new FakeHost(true, false, false);
        BrowserShellController controller = new BrowserShellController(manager, host);

        assertFalse(controller.requestHotkeyCapture());
        assertFalse(controller.hasPendingCaptureRequest());
        assertEquals(0, host.quickCaptureInteractionRequests);
    }

    @Test
    void hotkeyCaptureLeavesNoPendingRequestToExpire() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        BrowserShellController controller = new BrowserShellController(manager, new FakeHost(true, false, true));

        assertTrue(controller.requestHotkeyCapture());

        assertFalse(controller.hasPendingCaptureRequest());
    }

    @Test
    void externalCloseSignalDoesNotCancelDeferredCommandOpen() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        FakeHost host = new FakeHost(true, false, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        assertTrue(controller.requestBrowserOpenFromCommand());

        controller.handleExternalCloseSignal();
        controller.flushDeferredUiActions();

        assertEquals(BrowserState.OPEN_EMPTY, manager.getState());
        assertEquals(1, host.openBrowserRootRequests);
        assertTrue(host.browserRootActive);
    }

    @Test
    void externalCloseSignalDoesNotCloseActiveBrowserRoot() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.openEmptyBrowser();
        FakeHost host = new FakeHost(true, true, true);
        BrowserShellController controller = new BrowserShellController(manager, host);

        controller.handleExternalCloseSignal();

        assertEquals(BrowserState.OPEN_EMPTY, manager.getState());
        assertEquals(0, host.prepareToCloseBrowserRequests);
        assertEquals(0, host.closeCurrentScreenRequests);
        assertTrue(host.browserRootActive);
    }

    private static final class FakeHost implements BrowserShellHost {
        private final boolean inWorld;
        private final boolean canTriggerQuickCapture;
        private boolean browserRootActive;
        private int openBrowserRootRequests;
        private int prepareToCloseBrowserRequests;
        private int closeCurrentScreenRequests;
        private int quickCaptureInteractionRequests;

        private FakeHost(boolean inWorld, boolean browserRootActive, boolean canTriggerQuickCapture) {
            this.inWorld = inWorld;
            this.browserRootActive = browserRootActive;
            this.canTriggerQuickCapture = canTriggerQuickCapture;
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
        public boolean canTriggerQuickCapture() {
            return canTriggerQuickCapture;
        }

        @Override
        public void showBrowserRoot(BrowserShellController controller) {
            openBrowserRootRequests++;
            browserRootActive = true;
        }

        @Override
        public void prepareToCloseBrowser(BrowserShellController controller) {
            prepareToCloseBrowserRequests++;
        }

        @Override
        public void closeCurrentScreen() {
            closeCurrentScreenRequests++;
            browserRootActive = false;
        }

        @Override
        public void performQuickCaptureInteraction(BrowserShellController controller) {
            quickCaptureInteractionRequests++;
        }
    }
}