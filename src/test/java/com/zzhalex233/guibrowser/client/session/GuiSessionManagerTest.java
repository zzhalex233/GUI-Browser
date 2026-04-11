package com.zzhalex233.guibrowser.client.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
