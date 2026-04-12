package com.zzhalex233.guibrowser.client.history;

import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import net.minecraft.client.gui.GuiScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiHistoryStoreTest {

    @Test
    void historyRecordsSessionOpenHideAndDestroy() {
        GuiHistoryStore store = new GuiHistoryStore();
        GuiSessionManager manager = new GuiSessionManager(store);
        GuiSession session = manager.registerOpenedSession(new GuiScreen() {
        }, "Chat");

        manager.hideSession(session.getId());
        manager.destroySession(session.getId());

        assertEquals(3, store.entries().size());
        assertEquals(GuiHistoryEntry.Action.OPENED, store.entries().get(0).getAction());
        assertEquals(GuiHistoryEntry.Action.HIDDEN, store.entries().get(1).getAction());
        assertEquals(GuiHistoryEntry.Action.DESTROYED, store.entries().get(2).getAction());
    }

    @Test
    void historyRecordsActivation() {
        GuiHistoryStore store = new GuiHistoryStore();
        GuiSessionManager manager = new GuiSessionManager(store);
        GuiSession session = manager.registerOpenedSession(new GuiScreen() {
        }, "Chest");
        manager.hideSession(session.getId());

        manager.activateSession(session.getId());

        assertEquals(3, store.entries().size());
        assertEquals(GuiHistoryEntry.Action.ACTIVATED, store.entries().get(2).getAction());
    }

    @Test
    void worldUnloadRecordsClearEvent() {
        GuiHistoryStore store = new GuiHistoryStore();
        GuiSessionManager manager = new GuiSessionManager(store);
        manager.registerOpenedSession(new GuiScreen() {
        }, "Chest");

        manager.clearForWorldUnload();

        assertEquals(2, store.entries().size());
        assertEquals(GuiHistoryEntry.Action.CLEARED_ON_UNLOAD, store.entries().get(1).getAction());
    }

    @Test
    void historyRecordsTitleFromSession() {
        GuiHistoryStore store = new GuiHistoryStore();
        GuiSessionManager manager = new GuiSessionManager(store);
        manager.registerOpenedSession(new GuiScreen() {
        }, "My Chest");

        assertEquals("My Chest", store.entries().get(0).getSessionTitle());
    }

    @Test
    void storeWithoutManagerRecordsDirectly() {
        GuiHistoryStore store = new GuiHistoryStore();

        store.record("Test", GuiHistoryEntry.Action.OPENED);

        assertEquals(1, store.size());
    }

    @Test
    void clearRemovesAllEntries() {
        GuiHistoryStore store = new GuiHistoryStore();
        store.record("A", GuiHistoryEntry.Action.OPENED);
        store.record("B", GuiHistoryEntry.Action.HIDDEN);

        store.clear();

        assertEquals(0, store.size());
    }
}
