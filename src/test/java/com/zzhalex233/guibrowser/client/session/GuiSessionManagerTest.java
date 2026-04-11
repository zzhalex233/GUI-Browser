package com.zzhalex233.guibrowser.client.session;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiSessionManagerTest {

    @Test
    void registerForegroundSessionCreatesLiveTabForOriginalGuiInstance() {
        GuiSessionManager manager = new GuiSessionManager();
        Object gui = new Object();

        GuiSession session = manager.registerOpenedSession(gui, "Chat");

        assertSame(gui, session.getScreen());
        assertTrue(session.isForeground());
        assertFalse(session.isHidden());
        assertSame(session, manager.getForegroundSession());
    }

    @Test
    void hidingForegroundSessionKeepsOriginalInstanceCached() {
        GuiSessionManager manager = new GuiSessionManager();
        Object gui = new Object();
        GuiSession session = manager.registerOpenedSession(gui, "Chest");

        manager.hideSession(session.getId());

        assertTrue(manager.getSession(session.getId()).isHidden());
        assertSame(gui, manager.getSession(session.getId()).getScreen());
        assertNull(manager.getForegroundSession());
    }

    @Test
    void explicitTabCloseDestroysOnlyThatSession() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession first = manager.registerOpenedSession(new Object(), "A");
        GuiSession second = manager.registerOpenedSession(new Object(), "B");

        manager.destroySession(first.getId());

        assertNull(manager.findSession(first.getId()));
        assertNotNull(manager.findSession(second.getId()));
    }

    @Test
    void listVisibleTabsExcludesHiddenSessions() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession hidden = manager.registerOpenedSession(new Object(), "Hidden");
        manager.hideSession(hidden.getId());
        GuiSession visible = manager.registerOpenedSession(new Object(), "Visible");

        Collection<GuiSession> visibleTabs = manager.listVisibleTabs();

        assertEquals(1, visibleTabs.size());
        assertTrue(visibleTabs.contains(visible));
        assertFalse(visibleTabs.contains(hidden));
    }

    @Test
    void activateSessionRestoresHiddenSessionAndTracksLastActivation() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession first = manager.registerOpenedSession(new Object(), "First");
        manager.hideSession(first.getId());
        GuiSession second = manager.registerOpenedSession(new Object(), "Second");

        manager.activateSession(first.getId());

        assertSame(first, manager.getForegroundSession());
        assertFalse(first.isHidden());
        assertTrue(first.isForeground());
        assertFalse(second.isForeground());
        assertEquals(first.getId(), manager.getLastActivatedSessionId());
    }

    @Test
    void clearForWorldUnloadDestroysAllSessionsAndClearsForeground() {
        GuiSessionManager manager = new GuiSessionManager();
        manager.registerOpenedSession(new Object(), "A");
        manager.registerOpenedSession(new Object(), "B");

        manager.clearForWorldUnload();

        assertTrue(manager.listAllSessions().isEmpty());
        assertNull(manager.getForegroundSession());
        assertNull(manager.getLastActivatedSessionId());
    }
}