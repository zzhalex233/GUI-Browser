package com.zzhalex233.guibrowser.client.browser;

import com.zzhalex233.guibrowser.config.BrowserConfig;
import com.zzhalex233.guibrowser.config.EscAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void toggleBrowserAlternatesPhaseOneState() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());

        manager.toggleBrowser();
        assertEquals(BrowserState.OPEN_EMPTY, manager.getState());

        manager.toggleBrowser();
        assertEquals(BrowserState.CLOSED, manager.getState());
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
    void defaultsUseMinimizeEscAndPositiveOpenKeyCode() {
        BrowserConfig config = BrowserConfig.defaults();

        assertEquals(EscAction.MINIMIZE, config.getEscAction());
        assertTrue(config.getOpenBrowserKeyCode() > 0);
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
}