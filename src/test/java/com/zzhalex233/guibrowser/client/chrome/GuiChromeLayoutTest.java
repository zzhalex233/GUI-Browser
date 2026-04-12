package com.zzhalex233.guibrowser.client.chrome;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiChromeLayoutTest {

    @Test
    void tabsLayOutLeftToRightAcrossTopBar() {
        GuiChromeLayout layout = new GuiChromeLayout(320, 240);

        GuiChromeLayout.Rect first = layout.tabRect(0);
        GuiChromeLayout.Rect second = layout.tabRect(1);

        assertTrue(second.getX() > first.getRight());
        assertEquals(layout.topBarRect().getY(), first.getY());
    }

    @Test
    void bookmarkAndHistoryButtonsStayOnRightEdge() {
        GuiChromeLayout layout = new GuiChromeLayout(320, 240);

        assertTrue(layout.historyButtonRect().getRight() <= 320);
        assertTrue(layout.bookmarkButtonRect().getRight() < layout.historyButtonRect().getX());
    }

    @Test
    void topBarSpansFullScreenWidth() {
        GuiChromeLayout layout = new GuiChromeLayout(320, 240);

        GuiChromeLayout.Rect bar = layout.topBarRect();

        assertEquals(0, bar.getX());
        assertEquals(320, bar.getWidth());
        assertEquals(0, bar.getY());
        assertTrue(bar.getHeight() > 0);
    }

    @Test
    void tabCloseHotspotIsInsideTabRect() {
        GuiChromeLayout layout = new GuiChromeLayout(320, 240);

        GuiChromeLayout.Rect tab = layout.tabRect(0);
        GuiChromeLayout.Rect close = layout.tabCloseRect(0);

        assertTrue(close.getX() >= tab.getX());
        assertTrue(close.getRight() <= tab.getRight());
        assertTrue(close.getY() >= tab.getY());
        assertTrue(close.getBottom() <= tab.getBottom());
    }

    @Test
    void hitTestResolvesTabTarget() {
        GuiChromeLayout layout = new GuiChromeLayout(320, 240);

        GuiChromeLayout.Rect tab = layout.tabRect(0);
        int cx = tab.getX() + 5;
        int cy = tab.getY() + tab.getHeight() / 2;

        GuiChromeTarget target = layout.hitTest(cx, cy, 1);

        assertEquals(GuiChromeTarget.Type.TAB, target.getType());
        assertEquals(0, target.getTabIndex());
    }

    @Test
    void hitTestResolvesTabCloseTarget() {
        GuiChromeLayout layout = new GuiChromeLayout(320, 240);

        GuiChromeLayout.Rect close = layout.tabCloseRect(0);
        int cx = close.getX() + close.getWidth() / 2;
        int cy = close.getY() + close.getHeight() / 2;

        GuiChromeTarget target = layout.hitTest(cx, cy, 1);

        assertEquals(GuiChromeTarget.Type.TAB_CLOSE, target.getType());
        assertEquals(0, target.getTabIndex());
    }

    @Test
    void hitTestResolvesBookmarkButton() {
        GuiChromeLayout layout = new GuiChromeLayout(320, 240);

        GuiChromeLayout.Rect bookmark = layout.bookmarkButtonRect();
        int cx = bookmark.getX() + bookmark.getWidth() / 2;
        int cy = bookmark.getY() + bookmark.getHeight() / 2;

        GuiChromeTarget target = layout.hitTest(cx, cy, 0);

        assertEquals(GuiChromeTarget.Type.BOOKMARK_BUTTON, target.getType());
    }

    @Test
    void hitTestResolvesHistoryButton() {
        GuiChromeLayout layout = new GuiChromeLayout(320, 240);

        GuiChromeLayout.Rect history = layout.historyButtonRect();
        int cx = history.getX() + history.getWidth() / 2;
        int cy = history.getY() + history.getHeight() / 2;

        GuiChromeTarget target = layout.hitTest(cx, cy, 0);

        assertEquals(GuiChromeTarget.Type.HISTORY_BUTTON, target.getType());
    }

    @Test
    void hitTestReturnNoneForAreaBelowTopBar() {
        GuiChromeLayout layout = new GuiChromeLayout(320, 240);

        GuiChromeTarget target = layout.hitTest(160, 200, 0);

        assertEquals(GuiChromeTarget.Type.NONE, target.getType());
    }

    @Test
    void hitTestReturnNoneForEmptyTopBarAreaBeyondTabs() {
        GuiChromeLayout layout = new GuiChromeLayout(800, 600);

        // Click in top bar, past last tab (tab 1 ends at 122), before buttons (starting at ~772)
        int cx = 200;
        int cy = layout.topBarRect().getY() + layout.topBarRect().getHeight() / 2;

        GuiChromeTarget target = layout.hitTest(cx, cy, 2);

        assertEquals(GuiChromeTarget.Type.NONE, target.getType());
    }

    @Test
    void topBarHeightIsAccessible() {
        GuiChromeLayout layout = new GuiChromeLayout(320, 240);

        assertTrue(layout.getTopBarHeight() > 0);
        assertEquals(layout.topBarRect().getHeight(), layout.getTopBarHeight());
    }

    @Test
    void rectContainsPointInsideBounds() {
        GuiChromeLayout.Rect rect = new GuiChromeLayout.Rect(10, 20, 50, 30);

        assertTrue(rect.contains(10, 20));
        assertTrue(rect.contains(35, 35));
        assertTrue(rect.contains(59, 49));
        assertFalse(rect.contains(60, 50));
        assertFalse(rect.contains(9, 20));
        assertFalse(rect.contains(10, 19));
    }
}
