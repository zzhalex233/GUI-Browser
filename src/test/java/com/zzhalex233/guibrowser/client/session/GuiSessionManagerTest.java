package com.zzhalex233.guibrowser.client.session;

import org.junit.jupiter.api.Test;

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
    }

    @Test
    void hidingForegroundSessionKeepsOriginalInstanceCached() {
        GuiSessionManager manager = new GuiSessionManager();
        Object gui = new Object();
        GuiSession session = manager.registerOpenedSession(gui, "Chest");

        manager.hideSession(session.getId());

        assertTrue(manager.getSession(session.getId()).isHidden());
        assertSame(gui, manager.getSession(session.getId()).getScreen());
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
}