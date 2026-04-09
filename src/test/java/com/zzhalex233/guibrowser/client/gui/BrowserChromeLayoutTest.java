package com.zzhalex233.guibrowser.client.gui;

import com.zzhalex233.guibrowser.client.browser.BrowserWindowState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserChromeLayoutTest {

    @Test
    void titleBarContainsPointInsideWindow() {
        BrowserWindowState window = new BrowserWindowState(40, 30, 360, 240);

        assertTrue(BrowserChromeLayout.titleBar(window).contains(60, 40));
    }

    @Test
    void contentAreaStaysBelowTabStrip() {
        BrowserWindowState window = new BrowserWindowState(40, 30, 360, 240);

        assertTrue(BrowserChromeLayout.contentArea(window).getY()
            > BrowserChromeLayout.tabStrip(window).getBottom());
    }

    @Test
    void closeButtonStaysInsideTitleBar() {
        BrowserWindowState window = new BrowserWindowState(40, 30, 360, 240);

        BrowserChromeLayout.Rect titleBar = BrowserChromeLayout.titleBar(window);
        BrowserChromeLayout.Rect closeButton = BrowserChromeLayout.closeButton(window);

        assertTrue(titleBar.contains(closeButton.getX(), closeButton.getY()));
        assertEquals(titleBar.getY() + 4, closeButton.getY());
    }
}
