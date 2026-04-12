package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiSessionManagerTest {

    @Test
    void registerForegroundSessionCreatesLiveTabForOriginalGuiInstance() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiScreen gui = new GuiScreen() {
        };

        GuiSession session = manager.registerOpenedSession(gui, "Chest");

        assertSame(gui, session.getScreen());
        assertTrue(session.isForeground());
        assertFalse(session.isHidden());
        assertSame(session, manager.getForegroundSession());
    }

    @Test
    void resolveFallsBackToGuiForBlankTitleAndAnonymousScreen() {
        GuiScreen gui = new GuiScreen() {
        };

        assertEquals("GUI", GuiSessionTitleResolver.resolve(gui, "   "));
    }

    @Test
    void hidingForegroundSessionKeepsOriginalInstanceCached() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiScreen gui = new GuiScreen() {
        };
        GuiSession session = manager.registerOpenedSession(gui, "Chest");

        manager.hideSession(session.getId());

        assertTrue(manager.getSession(session.getId()).isHidden());
        assertSame(gui, manager.getSession(session.getId()).getScreen());
    }

    @Test
    void explicitTabCloseDestroysOnlyThatSession() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession first = manager.registerOpenedSession(new GuiScreen() {
        }, "Chat");
        GuiSession second = manager.registerOpenedSession(new GuiScreen() {
        }, "Chat 2");

        manager.destroySession(first.getId());

        assertNull(manager.findSession(first.getId()));
        assertSame(second, manager.findSession(second.getId()));
    }

    @Test
    void activateSessionBringsHiddenSessionBackForeground() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession first = manager.registerOpenedSession(new GuiScreen() {
        }, "Chest");
        manager.hideSession(first.getId());

        manager.activateSession(first.getId());

        assertTrue(manager.getSession(first.getId()).isForeground());
        assertFalse(manager.getSession(first.getId()).isHidden());
        assertSame(first, manager.getForegroundSession());
    }

    @Test
    void listVisibleTabsExcludesHiddenSessionsButKeepsInsertionOrder() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession first = manager.registerOpenedSession(new GuiScreen() {
        }, "First");
        GuiSession second = manager.registerOpenedSession(new GuiScreen() {
        }, "Second");
        manager.hideSession(second.getId());
        GuiSession third = manager.registerOpenedSession(new GuiScreen() {
        }, "Third");

        assertEquals(2, manager.listVisibleTabs().size());
        assertSame(first, manager.listVisibleTabs().get(0));
        assertSame(third, manager.listVisibleTabs().get(1));
    }

    @Test
    void clearForWorldUnloadRemovesAllSessionsAndForeground() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession session = manager.registerOpenedSession(new GuiScreen() {
        }, "Chest");
        manager.registerOpenedSession(new GuiScreen() {
        }, "Anvil");

        manager.clearForWorldUnload();

        assertNull(manager.getForegroundSession());
        assertTrue(manager.listVisibleTabs().isEmpty());
        assertNull(manager.findSession(session.getId()));
    }
}
