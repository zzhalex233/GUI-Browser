package com.zzhalex233.guibrowser.client.session;

import com.zzhalex233.guibrowser.client.popup.FakePopupScreen;
import com.zzhalex233.guibrowser.config.ContainerCacheMode;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiLifecycleBridgeTest {

    private static GuiSessionSource blockSource(int x, int y, int z) {
        return new GuiSessionSource.BlockSource(new BlockPos(x, y, z), 0);
    }

    private static void loadSource(InteractionSourceTracker tracker, GuiSessionSource source) {
        tracker.setPending(source, System.currentTimeMillis());
    }

    @Test
    void containerWithSourceCreatesSession() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiContainer container = new GuiContainer() {};
        GuiSessionSource source = blockSource(1, 2, 3);

        loadSource(sourceTracker, source);
        GuiLifecycleBridge.TransitionDecision decision = bridge.onBeforeDisplay(null, container, false);

        assertNotNull(decision.getActivatedSessionId());
        assertEquals(1, manager.listAllSessions().size());
        GuiSession session = manager.getForegroundSession();
        assertNotNull(session);
        assertSame(container, session.getScreen());
        assertEquals(source, session.getSource());
    }

    @Test
    void containerWithoutSourceIsExcluded() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiContainer container = new GuiContainer() {};

        GuiLifecycleBridge.TransitionDecision decision = bridge.onBeforeDisplay(null, container, false);

        assertNull(decision.getActivatedSessionId());
        assertTrue(manager.listAllSessions().isEmpty());
        assertNull(manager.getForegroundSession());
    }

    @Test
    void sameSourceReusesSession() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiContainer containerA = new GuiContainer() {};
        GuiContainer containerB = new GuiContainer() {};
        GuiSessionSource source = blockSource(1, 2, 3);

        loadSource(sourceTracker, source);
        bridge.onBeforeDisplay(null, containerA, false);
        GuiSessionId firstSessionId = manager.getForegroundSession().getId();

        loadSource(sourceTracker, source);
        bridge.onBeforeDisplay(containerA, containerB, false);

        GuiSession session = manager.getForegroundSession();
        assertNotNull(session);
        assertEquals(firstSessionId, session.getId());
        assertSame(containerB, session.getScreen());
        assertEquals(1, manager.listAllSessions().size());
    }

    @Test
    void hidingSessionOnEscDoesNotMarkStale() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiContainer container = new GuiContainer() {};
        GuiSessionSource source = blockSource(1, 2, 3);

        loadSource(sourceTracker, source);
        bridge.onBeforeDisplay(null, container, false);
        GuiSession session = manager.getForegroundSession();
        assertFalse(session.isStale());

        // ESC close (incoming == null) should NOT mark stale in HYBRID mode
        bridge.onBeforeDisplay(container, null, false);

        assertTrue(session.isHidden());
        assertFalse(session.isStale());
    }

    @Test
    void hidingSessionOnSwitchKeepsContainerLive() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiContainer containerA = new GuiContainer() {};
        GuiContainer containerB = new GuiContainer() {};
        GuiSessionSource sourceA = blockSource(1, 2, 3);
        GuiSessionSource sourceB = blockSource(4, 5, 6);

        loadSource(sourceTracker, sourceA);
        bridge.onBeforeDisplay(null, containerA, false);
        GuiSession session = manager.getForegroundSession();

        // Switching to another tracked GUI should keep the hidden container live.
        loadSource(sourceTracker, sourceB);
        bridge.onBeforeDisplay(containerA, containerB, false);

        assertTrue(session.isHidden());
        assertFalse(session.isStale());
    }

    @Test
    void openingPopupDoesNotMarkTrackedSessionStale() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiContainer container = new GuiContainer() {};
        GuiSessionSource source = blockSource(1, 2, 3);

        loadSource(sourceTracker, source);
        bridge.onBeforeDisplay(null, container, false);
        GuiSession session = manager.getForegroundSession();
        assertNotNull(session);

        bridge.onBeforeDisplay(container, new FakePopupScreen(), false);

        assertTrue(session.isHidden());
        assertFalse(session.isStale());
    }

    @Test
    void hybridModeSuppressesClose() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiContainer container = new GuiContainer() {};
        GuiSessionSource source = blockSource(1, 2, 3);

        loadSource(sourceTracker, source);
        bridge.onBeforeDisplay(null, container, false);

        GuiLifecycleBridge.TransitionDecision decision = bridge.onBeforeDisplay(container, null, false);

        assertTrue(decision.shouldSuppressCurrentClose());
        assertNotNull(decision.getHiddenSessionId());
    }

    @Test
    void visualOnlyModeDoesNotSuppressClose() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.VISUAL_ONLY);
        GuiContainer container = new GuiContainer() {};
        GuiSessionSource source = blockSource(1, 2, 3);

        loadSource(sourceTracker, source);
        bridge.onBeforeDisplay(null, container, false);

        GuiLifecycleBridge.TransitionDecision decision = bridge.onBeforeDisplay(container, null, false);

        assertFalse(decision.shouldSuppressCurrentClose());
        assertNotNull(decision.getHiddenSessionId());
        assertTrue(manager.findSession(decision.getHiddenSessionId()).isHidden());
    }

    @Test
    void explicitDestroyRemovesSessionInsteadOfHiding() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiContainer container = new GuiContainer() {};
        GuiSessionSource source = blockSource(1, 2, 3);

        loadSource(sourceTracker, source);
        bridge.onBeforeDisplay(null, container, false);
        GuiSession session = manager.getForegroundSession();

        GuiLifecycleBridge.TransitionDecision decision = bridge.onBeforeDisplay(container, null, true);

        assertFalse(decision.shouldSuppressCurrentClose());
        assertNull(decision.getHiddenSessionId());
        assertNull(manager.findSession(session.getId()));
    }

    @Test
    void worldUnloadClearsAllSessions() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiContainer container = new GuiContainer() {};

        loadSource(sourceTracker, blockSource(1, 2, 3));
        bridge.onBeforeDisplay(null, container, false);
        bridge.onWorldUnload();

        assertTrue(manager.listAllSessions().isEmpty());
        assertNull(manager.getForegroundSession());
    }

    @Test
    void destroySessionFromTabRemovesOnlyRequestedSession() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiSession first = manager.registerOpenedSession(new GuiContainer() {}, "First");
        GuiSession second = manager.registerOpenedSession(new GuiContainer() {}, "Second");

        bridge.destroySessionFromTab(first.getId());

        assertNull(manager.findSession(first.getId()));
        assertSame(second, manager.findSession(second.getId()));
    }

    @Test
    void destroyForegroundSessionRemovesOnlyForegroundSession() {
        GuiSessionManager manager = new GuiSessionManager();
        InteractionSourceTracker sourceTracker = new InteractionSourceTracker();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager, sourceTracker, ContainerCacheMode.HYBRID);
        GuiSession first = manager.registerOpenedSession(new GuiContainer() {}, "First");
        GuiSession second = manager.registerOpenedSession(new GuiContainer() {}, "Second");

        bridge.destroyForegroundSession();

        assertSame(first, manager.findSession(first.getId()));
        assertNull(manager.findSession(second.getId()));
        assertNull(manager.getForegroundSession());
    }

    @Test
    void backwardCompatConstructorUsesHybridMode() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);
        GuiContainer container = new GuiContainer() {};

        // Pre-register session manually (simulates external registration)
        GuiSession session = manager.registerOpenedSession(container, "Test");

        GuiLifecycleBridge.TransitionDecision decision = bridge.onBeforeDisplay(container, null, false);

        // HYBRID mode suppresses close for existing sessions
        assertTrue(decision.shouldSuppressCurrentClose());
        assertEquals(session.getId(), decision.getHiddenSessionId());
        // ESC close (incoming == null) does not mark stale
        assertFalse(session.isStale());
    }
}
