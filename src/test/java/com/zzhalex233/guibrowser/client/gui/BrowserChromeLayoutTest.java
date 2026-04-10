package com.zzhalex233.guibrowser.client.gui;

import com.zzhalex233.guibrowser.client.browser.BrowserWindowState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserChromeLayoutTest {

    @Test
    void frameMatchesBrowserPngSize() {
        BrowserWindowState window = BrowserWindowState.defaultWindow();
        BrowserChromeLayout.Rect frame = BrowserChromeLayout.frame(window);

        assertEquals(176, frame.getWidth());
        assertEquals(190, frame.getHeight());
    }

    @Test
    void contentAreaMatchesBrowserPngPageRegion() {
        BrowserWindowState window = BrowserWindowState.defaultWindow();
        BrowserChromeLayout.Rect content = BrowserChromeLayout.contentArea(window);

        assertEquals(window.getX(), content.getX());
        assertEquals(window.getY() + 24, content.getY());
        assertEquals(176, content.getWidth());
        assertEquals(166, content.getHeight());
    }

    @Test
    void firstTabAnchorsToBrowserLocalBottomLeftAtY27() {
        BrowserWindowState window = BrowserWindowState.defaultWindow();
        BrowserChromeLayout.Rect tab = BrowserChromeLayout.tab(window, 0);

        assertEquals(window.getX(), tab.getX());
        assertEquals(window.getY() + 4, tab.getY());
        assertEquals(28, tab.getWidth());
        assertEquals(23, tab.getHeight());
    }

    @Test
    void topRightButtonsStayContiguous() {
        BrowserWindowState window = BrowserWindowState.defaultWindow();
        BrowserChromeLayout.Rect minimize = BrowserChromeLayout.minimizeButton(window);
        BrowserChromeLayout.Rect close = BrowserChromeLayout.closeButton(window);

        assertEquals(window.getX() + 148, minimize.getX());
        assertEquals(window.getY() + 13, minimize.getY());
        assertEquals(minimize.getRight(), close.getX());
        assertTrue(close.getRight() <= BrowserChromeLayout.frame(window).getRight());
    }
}