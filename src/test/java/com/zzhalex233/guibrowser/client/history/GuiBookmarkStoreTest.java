package com.zzhalex233.guibrowser.client.history;

import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import net.minecraft.client.gui.GuiScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBookmarkStoreTest {

    @Test
    void bookmarkingActiveSessionMarksItPinnedWithoutDestroyingInstance() {
        GuiBookmarkStore store = new GuiBookmarkStore();
        GuiSessionManager manager = new GuiSessionManager(new GuiHistoryStore(), store);
        GuiSession session = manager.registerOpenedSession(new GuiScreen() {
        }, "Chat");

        manager.toggleBookmark(session.getId());

        assertTrue(store.isBookmarked(session.getId()));
        assertSame(session.getScreen(), manager.getForegroundSession().getScreen());
    }

    @Test
    void togglingBookmarkTwiceRemovesIt() {
        GuiBookmarkStore store = new GuiBookmarkStore();
        GuiSessionManager manager = new GuiSessionManager(new GuiHistoryStore(), store);
        GuiSession session = manager.registerOpenedSession(new GuiScreen() {
        }, "Chat");

        manager.toggleBookmark(session.getId());
        manager.toggleBookmark(session.getId());

        assertFalse(store.isBookmarked(session.getId()));
    }

    @Test
    void bookmarkStoreRecordsTitle() {
        GuiBookmarkStore store = new GuiBookmarkStore();
        GuiSessionManager manager = new GuiSessionManager(new GuiHistoryStore(), store);
        GuiSession session = manager.registerOpenedSession(new GuiScreen() {
        }, "My Chest");

        manager.toggleBookmark(session.getId());

        GuiBookmarkEntry entry = store.allBookmarks().get(session.getId());
        assertEquals("My Chest", entry.getSessionTitle());
    }

    @Test
    void worldUnloadClearsBookmarks() {
        GuiBookmarkStore store = new GuiBookmarkStore();
        GuiSessionManager manager = new GuiSessionManager(new GuiHistoryStore(), store);
        GuiSession session = manager.registerOpenedSession(new GuiScreen() {
        }, "Chat");
        manager.toggleBookmark(session.getId());

        manager.clearForWorldUnload();

        assertEquals(0, store.size());
    }

    @Test
    void directStoreOperationsWork() {
        GuiBookmarkStore store = new GuiBookmarkStore();
        com.zzhalex233.guibrowser.client.session.GuiSessionId id =
                com.zzhalex233.guibrowser.client.session.GuiSessionId.create();

        store.addBookmark(id, "Test", "TestClass");

        assertTrue(store.isBookmarked(id));
        assertEquals(1, store.size());

        store.removeBookmark(id);

        assertFalse(store.isBookmarked(id));
        assertEquals(0, store.size());
    }
}
