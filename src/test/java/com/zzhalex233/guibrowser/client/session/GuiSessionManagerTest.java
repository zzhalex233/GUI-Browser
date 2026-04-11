package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
}