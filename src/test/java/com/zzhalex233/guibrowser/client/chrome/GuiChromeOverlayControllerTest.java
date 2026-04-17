package com.zzhalex233.guibrowser.client.chrome;

import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiChromeOverlayControllerTest {

    @Test
    void middleClickTabRequestsExplicitDestroy() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiChromeOverlayController controller = new GuiChromeOverlayController(manager);
        GuiSession session = manager.registerOpenedSession(new GuiScreen() {
        }, "Chat");

        controller.handleTabMiddleClick(session.getId());

        assertNull(manager.findSession(session.getId()));
    }

    @Test
    void leftClickTabMarksThatSessionForeground() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiChromeOverlayController controller = new GuiChromeOverlayController(manager);
        GuiSession first = manager.registerOpenedSession(new GuiScreen() {
        }, "A");
        GuiSession second = manager.registerOpenedSession(new GuiScreen() {
        }, "B");

        controller.handleTabLeftClick(first.getId());

        assertEquals(first.getId(), manager.getForegroundSession().getId());
    }

    @Test
    void leftClickOnAlreadyForegroundTabDoesNothing() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiChromeOverlayController controller = new GuiChromeOverlayController(manager);
        GuiSession session = manager.registerOpenedSession(new GuiScreen() {
        }, "Chat");

        controller.handleTabLeftClick(session.getId());

        assertSame(session, manager.getForegroundSession());
    }

    @Test
    void leftClickOnLiveContainerTabDirectSwitchesWithoutStaleRestore() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiChromeOverlayController controller = new GuiChromeOverlayController(manager);
        GuiSessionSource source = new GuiSessionSource.BlockSource(new BlockPos(1, 64, 1), 0);
        GuiSession first = manager.registerOrReuseSession(new GuiContainer() {
        }, "First", source);
        GuiSession second = manager.registerOpenedSession(new GuiScreen() {
        }, "Second");
        manager.hideSession(first.getId());

        GuiChromeOverlayController.TabSwitchResult result = controller.handleTabLeftClick(first.getId());

        assertEquals(GuiChromeOverlayController.TabSwitchResult.DIRECT_SWITCH, result);
        assertSame(first, manager.getForegroundSession());
        assertFalse(first.isStale());
        assertSame(second, manager.findSession(second.getId()));
    }

    @Test
    void middleClickDestroyingForegroundClearsForeground() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiChromeOverlayController controller = new GuiChromeOverlayController(manager);
        GuiSession only = manager.registerOpenedSession(new GuiScreen() {
        }, "Only");

        controller.handleTabMiddleClick(only.getId());

        assertNull(manager.getForegroundSession());
        assertTrue(manager.listAllSessions().isEmpty());
    }

    @Test
    void getTabsReflectsCurrentSessionState() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiChromeOverlayController controller = new GuiChromeOverlayController(manager);
        manager.registerOpenedSession(new GuiScreen() {
        }, "A");
        manager.registerOpenedSession(new GuiScreen() {
        }, "B");

        List<GuiSession> tabs = controller.getTabs();

        assertEquals(2, tabs.size());
    }

    @Test
    void resolveMouseClickDelegatesToLayoutHitTest() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiChromeOverlayController controller = new GuiChromeOverlayController(manager);
        manager.registerOpenedSession(new GuiScreen() {
        }, "First");

        GuiChromeLayout layout = new GuiChromeLayout(320, 240);
        GuiChromeLayout.Rect tab = layout.tabRect(0);
        int cx = tab.getX() + 5;
        int cy = tab.getY() + tab.getHeight() / 2;

        GuiChromeTarget target = controller.resolveTarget(cx, cy, 320, 240);

        assertEquals(GuiChromeTarget.Type.TAB, target.getType());
        assertEquals(0, target.getTabIndex());
    }

    @Test
    void isInsideTopBarReturnsTrueForPointsInBar() {
        GuiChromeOverlayController controller = new GuiChromeOverlayController(new GuiSessionManager());

        assertTrue(controller.isInsideTopBar(10, 5, 320, 240));
    }
}
