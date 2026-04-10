package com.zzhalex233.guibrowser.client.input;

import com.zzhalex233.guibrowser.client.browser.BrowserManager;
import com.zzhalex233.guibrowser.client.browser.BrowserShellController;
import com.zzhalex233.guibrowser.client.browser.BrowserShellHost;
import com.zzhalex233.guibrowser.client.browser.BrowserState;
import com.zzhalex233.guibrowser.config.BrowserConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserHotkeyHandlerTest {

    @Test
    void hotkeyCaptureTriggersSingleQuickInteractionAndDoesNotOpenEmptyBrowser() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        FakeHost host = new FakeHost();
        BrowserShellController controller = new BrowserShellController(manager, host);

        assertTrue(controller.requestHotkeyCapture());

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertEquals(1, host.quickCaptureRequests);
    }

    @Test
    void hotkeyCaptureReturnsFalseWhenWorldInteractionIsUnavailable() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        FakeHost host = new FakeHost();
        host.canTriggerQuickCapture = false;
        BrowserShellController controller = new BrowserShellController(manager, host);

        assertFalse(controller.requestHotkeyCapture());

        assertEquals(0, host.quickCaptureRequests);
        assertFalse(controller.hasPendingCaptureRequest());
    }

    private static final class FakeHost implements BrowserShellHost {
        private boolean canTriggerQuickCapture = true;
        private int quickCaptureRequests;

        @Override
        public boolean isInWorld() {
            return true;
        }

        @Override
        public boolean isBrowserRootActive() {
            return false;
        }

        @Override
        public boolean canTriggerQuickCapture() {
            return canTriggerQuickCapture;
        }

        @Override
        public void showBrowserRoot(BrowserShellController controller) {
        }

        @Override
        public void prepareToCloseBrowser(BrowserShellController controller) {
        }

        @Override
        public void closeCurrentScreen() {
        }

        @Override
        public void performQuickCaptureInteraction(BrowserShellController controller) {
            quickCaptureRequests++;
        }
    }
}