package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiLifecycleBridgeTest {

    @Test
    void closingTrackedScreenHidesSessionInsteadOfDestroyingIt() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);
        GuiScreen current = new GuiChat();
        GuiSession session = manager.registerOpenedSession(current, "Chat");

        GuiLifecycleBridge.TransitionDecision decision = bridge.onBeforeDisplay(current, null, false);

        assertTrue(decision.shouldSuppressCurrentClose());
        assertEquals(session.getId(), decision.getHiddenSessionId());
        assertTrue(manager.getSession(session.getId()).isHidden());
        assertSame(current, manager.getSession(session.getId()).getScreen());
        assertNull(manager.getForegroundSession());
    }

    @Test
    void openingNewTrackedScreenKeepsPreviousSessionCached() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);
        GuiScreen first = new GuiChat();
        GuiScreen second = new GuiChat();

        bridge.onBeforeDisplay(null, first, false);
        GuiLifecycleBridge.TransitionDecision decision = bridge.onBeforeDisplay(first, second, false);

        assertEquals(2, manager.listAllSessions().size());
        assertTrue(manager.findSessionByScreen(first).isHidden());
        assertSame(second, manager.getForegroundSession().getScreen());
        assertEquals(manager.findSessionByScreen(second).getId(), decision.getActivatedSessionId());
    }

    @Test
    void reopeningCachedScreenReactivatesExistingSession() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);
        GuiScreen first = new GuiChat();
        GuiScreen second = new GuiChat();

        bridge.onBeforeDisplay(null, first, false);
        bridge.onBeforeDisplay(first, second, false);
        bridge.onBeforeDisplay(second, first, false);

        assertEquals(2, manager.listAllSessions().size());
        assertFalse(manager.findSessionByScreen(first).isHidden());
        assertTrue(manager.findSessionByScreen(second).isHidden());
        assertSame(first, manager.getForegroundSession().getScreen());
    }

    @Test
    void nonTrackedIncomingScreenDoesNotCreateForegroundSession() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);
        GuiScreen tracked = new GuiChat();

        bridge.onBeforeDisplay(null, tracked, false);
        GuiLifecycleBridge.TransitionDecision decision = bridge.onBeforeDisplay(tracked, new GuiMainMenu(), false);

        assertTrue(decision.shouldSuppressCurrentClose());
        assertNull(decision.getActivatedSessionId());
        assertEquals(1, manager.listAllSessions().size());
        assertTrue(manager.findSessionByScreen(tracked).isHidden());
        assertNull(manager.getForegroundSession());
    }

    @Test
    void worldUnloadClearsAllSessions() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);

        bridge.onBeforeDisplay(null, new GuiChat(), false);
        bridge.clearForWorldUnload();

        assertTrue(manager.listAllSessions().isEmpty());
        assertNull(manager.getForegroundSession());
    }

    @Test
    void explicitTabDestroyRemovesOnlyRequestedSession() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);
        GuiSession first = manager.registerOpenedSession(new GuiChat(), "First");
        GuiSession second = manager.registerOpenedSession(new GuiChat(), "Second");

        bridge.destroySessionFromTab(first.getId());

        assertNull(manager.findSession(first.getId()));
        assertSame(second, manager.findSession(second.getId()));
    }

    @Test
    void destroyForegroundSessionRemovesOnlyForegroundSession() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);
        GuiSession first = manager.registerOpenedSession(new GuiChat(), "First");
        GuiSession second = manager.registerOpenedSession(new GuiChat(), "Second");

        bridge.destroyForegroundSession();

        assertSame(first, manager.findSession(first.getId()));
        assertNull(manager.findSession(second.getId()));
        assertNull(manager.getForegroundSession());
    }
}
