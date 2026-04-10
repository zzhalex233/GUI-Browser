package com.zzhalex233.guibrowser.client.browser;

import com.zzhalex233.guibrowser.config.BrowserConfig;
import com.zzhalex233.guibrowser.config.EscAction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserManagerTest {

    @Test
    void openEmptyBrowserTransitionsFromClosed() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());

        assertEquals(BrowserState.CLOSED, manager.getState());

        manager.openEmptyBrowser();

        assertEquals(BrowserState.OPEN_EMPTY, manager.getState());
    }

    @Test
    void closeBrowserTransitionsBackToClosed() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.openEmptyBrowser();

        manager.closeBrowser();

        assertEquals(BrowserState.CLOSED, manager.getState());
    }

    @Test
    void toggleBrowserOpensFromClosedAndRestoresFromMinimized() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        Object hosted = new Object();

        manager.toggleBrowser();
        assertEquals(BrowserState.OPEN_EMPTY, manager.getState());

        manager.captureHostedContent(hosted, "Chest");
        manager.minimizeBrowser();
        manager.toggleBrowser();

        assertEquals(BrowserState.OPEN_WITH_TABS, manager.getState());
        assertSame(hosted, manager.getHostedContent());
    }

    @Test
    void handleEscFromRootClosesOpenEmptyBrowser() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.openEmptyBrowser();
        manager.getWindowState().beginDrag(20, 10, 20, 10);

        manager.handleEscFromRoot();

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertFalse(manager.getWindowState().isDragging());
    }

    @Test
    void handleEscFromRootMinimizesHostedContentWhenConfigured() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        Object hosted = new Object();

        manager.captureHostedContent(hosted, "Chest");
        manager.handleEscFromRoot();

        assertEquals(BrowserState.MINIMIZED_WITH_TABS, manager.getState());
        assertSame(hosted, manager.getHostedContent());
    }

    @Test
    void handleEscFromRootClosesHostedContentWhenConfiguredToClose() {
        BrowserConfig config = BrowserConfig.defaults().withEscAction(EscAction.CLOSE);
        BrowserManager manager = BrowserManager.createForTests(config);

        manager.captureHostedContent(new Object(), "Chest");
        manager.handleEscFromRoot();

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertFalse(manager.hasHostedContent());
    }

    @Test
    void armingCaptureDoesNotOpenTheBrowserByItself() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());

        manager.armCaptureRequest(BrowserCaptureRequest.hotkeyRequest());

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertTrue(manager.hasPendingCaptureRequest());
    }

    @Test
    void captureRequestExpiresAfterTimeout() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.armCaptureRequest(BrowserCaptureRequest.hotkeyRequest());

        for (int i = 0; i < BrowserCaptureRequest.DEFAULT_TICKS_TO_LIVE; i++) {
            manager.tickCaptureRequest();
        }

        assertFalse(manager.hasPendingCaptureRequest());
    }

    @Test
    void defaultsUseMinimizeEscAndPositiveOpenKeyCode() {
        BrowserConfig config = BrowserConfig.defaults();

        assertEquals(EscAction.MINIMIZE, config.getEscAction());
        assertTrue(config.getCaptureHotkeyKeyCode() > 0);
    }

    @Test
    void draggingWindowKeepsTitleBarReachableAfterOffscreenDrag() {
        BrowserWindowState window = BrowserWindowState.defaultWindow();

        window.beginDrag(20, 10, 20, 10);
        window.dragTo(-500, -500, 320, 240);

        assertEquals(0, window.getX());
        assertEquals(0, window.getY());
        assertTrue(window.isDragging());
        window.endDrag();
        assertFalse(window.isDragging());
    }

    @Test
    void clampToViewportKeepsWindowReachableAfterResize() {
        BrowserWindowState window = new BrowserWindowState(500, 400, 360, 240);

        window.clampToViewport(320, 240);

        assertEquals(0, window.getX());
        assertEquals(0, window.getY());
    }

    @Test
    void captureHostedContentTransitionsToOpenWithTabsAndStoresReference() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        Object hostedContent = new Object();

        manager.captureHostedContent(hostedContent, "Chest");

        assertEquals(BrowserState.OPEN_WITH_TABS, manager.getState());
        assertTrue(manager.hasHostedContent());
        assertSame(hostedContent, manager.getHostedContent());
        assertEquals("Chest", manager.getActiveTabTitle());
    }

    @Test
    void clearHostedContentReturnsToOpenEmptyState() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.captureHostedContent(new Object(), "Chest");

        manager.clearHostedContent();

        assertEquals(BrowserState.OPEN_EMPTY, manager.getState());
        assertFalse(manager.hasHostedContent());
    }

    @Test
    void minimizePreservesHostedContentForRestore() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        Object hosted = new Object();

        manager.captureHostedContent(hosted, "Chest");
        manager.minimizeBrowser();

        assertEquals(BrowserState.MINIMIZED_WITH_TABS, manager.getState());
        assertSame(hosted, manager.getHostedContent());

        manager.restoreBrowser();

        assertEquals(BrowserState.OPEN_WITH_TABS, manager.getState());
        assertSame(hosted, manager.getHostedContent());
    }

    @Test
    void closeBrowserClearsHostedContentAndRestoreData() throws ReflectiveOperationException {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        Object hosted = new Object();

        manager.captureHostedContent(hosted, "Chest");
        manager.minimizeBrowser();
        manager.armCaptureRequest(BrowserCaptureRequest.hotkeyRequest());
        manager.closeBrowser();
        manager.restoreBrowser();

        assertEquals(BrowserState.CLOSED, manager.getState());
        assertFalse(manager.hasHostedContent());
        assertSame(null, readField(manager, "lastMinimizedHostedContent"));
        assertSame(null, readField(manager, "lastMinimizedTabTitle"));
        assertSame(null, readField(manager, "captureRequest"));
        assertFalse(manager.getState() == BrowserState.OPEN_WITH_TABS);
        assertFalse(manager.getHostedContent() == hosted);
    }

    private static Object readField(BrowserManager manager, String fieldName) throws ReflectiveOperationException {
        Field field = BrowserManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(manager);
    }
}