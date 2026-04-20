package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
        assertEquals(session.getId(), manager.getLastActivatedSessionId());
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
        assertEquals(session.getId(), manager.getLastActivatedSessionId());
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
        assertEquals(first.getId(), manager.getLastActivatedSessionId());
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
        assertNull(manager.getLastActivatedSessionId());
        assertTrue(manager.listVisibleTabs().isEmpty());
        assertNull(manager.findSession(session.getId()));
    }

    @Test
    void destroyingLastActivatedForegroundClearsTracking() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession second = manager.registerOpenedSession(new GuiScreen() {
        }, "Second");

        manager.destroySession(second.getId());

        assertNull(manager.getForegroundSession());
        assertNull(manager.getLastActivatedSessionId());
    }

    @Test
    void destroyingLastActivatedSessionFallsBackToMostRecentlyActivatedRemainingSession() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession first = manager.registerOpenedSession(new GuiScreen() {
        }, "First");
        GuiSession second = manager.registerOpenedSession(new GuiScreen() {
        }, "Second");

        manager.activateSession(first.getId());
        manager.destroySession(first.getId());

        assertNull(manager.getForegroundSession());
        assertEquals(second.getId(), manager.getLastActivatedSessionId());
    }

    @Test
    void registerWithSameSourceReusesExistingSession() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSessionSource source = new GuiSessionSource.BlockSource(new BlockPos(1, 2, 3), 0);
        GuiContainer gui1 = new GuiContainer();
        GuiContainer gui2 = new GuiContainer();

        GuiSession first = manager.registerOrReuseSession(gui1, "Chest", source);
        GuiSession second = manager.registerOrReuseSession(gui2, "Chest", source);

        assertSame(first, second);
        assertEquals(1, manager.listAllSessions().size());
        assertSame(gui2, first.getScreen());
    }

    @Test
    void registerWithDifferentSourcesCreatesSeparateSessions() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSessionSource sourceA = new GuiSessionSource.BlockSource(new BlockPos(1, 2, 3), 0);
        GuiSessionSource sourceB = new GuiSessionSource.BlockSource(new BlockPos(4, 5, 6), 0);
        GuiContainer guiA = new GuiContainer();
        GuiContainer guiB = new GuiContainer();

        GuiSession first = manager.registerOrReuseSession(guiA, "Chest A", sourceA);
        GuiSession second = manager.registerOrReuseSession(guiB, "Chest B", sourceB);

        assertNotSame(first, second);
        assertEquals(2, manager.listAllSessions().size());
    }

    @Test
    void destroySessionCleansSourceIndex() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSessionSource source = new GuiSessionSource.BlockSource(new BlockPos(1, 2, 3), 0);
        GuiContainer gui1 = new GuiContainer();
        GuiContainer gui2 = new GuiContainer();

        GuiSession first = manager.registerOrReuseSession(gui1, "Chest", source);
        manager.destroySession(first.getId());
        GuiSession second = manager.registerOrReuseSession(gui2, "Chest", source);

        assertNotSame(first, second);
        assertEquals(1, manager.listAllSessions().size());
    }

    @Test
    void registerWithNullSourceAlwaysCreatesNewSession() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiContainer gui1 = new GuiContainer();
        GuiContainer gui2 = new GuiContainer();

        GuiSession first = manager.registerOrReuseSession(gui1, "Chest", null);
        GuiSession second = manager.registerOrReuseSession(gui2, "Chest", null);

        assertNotSame(first, second);
        assertEquals(2, manager.listAllSessions().size());
    }

    @Test
    void renderableSessionFallsBackToForegroundForCurrentScreenDuringResync() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSessionSource source = new GuiSessionSource.BlockSource(new BlockPos(1, 2, 3), 0);
        GuiContainer originalScreen = new GuiContainer();
        GuiContainer replacementScreen = new GuiContainer();

        GuiSession session = manager.registerOrReuseSession(originalScreen, "Chest", source);
        session.updateScreen(replacementScreen);

        assertSame(session, manager.findRenderableSession(originalScreen, originalScreen));
        assertSame(session, manager.findRenderableSession(replacementScreen, replacementScreen));
    }

    @Test
    void renderableSessionDoesNotFallbackForNonCurrentScreen() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSessionSource source = new GuiSessionSource.BlockSource(new BlockPos(1, 2, 3), 0);
        GuiContainer originalScreen = new GuiContainer();
        GuiContainer replacementScreen = new GuiContainer();
        GuiScreen unrelatedCurrent = new GuiScreen() {
        };

        GuiSession session = manager.registerOrReuseSession(originalScreen, "Chest", source);
        session.updateScreen(replacementScreen);

        assertNull(manager.findRenderableSession(originalScreen, unrelatedCurrent));
    }
}
