package com.zzhalex233.guibrowser.client.history;

import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBookmarkStoreTest {

    private static GuiSessionSource testSource(int x) {
        return new GuiSessionSource.BlockSource(new BlockPos(x, 64, 0), 0);
    }

    @Test
    void bookmarkingActiveSessionMarksItPinnedWithoutDestroyingInstance() {
        GuiBookmarkStore store = new GuiBookmarkStore();
        GuiSessionManager manager = new GuiSessionManager(new GuiHistoryStore(), store);
        GuiSession session = manager.registerOrReuseSession(new GuiScreen() {
        }, "Chat", testSource(1));

        manager.toggleBookmark(session.getId());

        assertTrue(manager.isSessionBookmarked(session.getId()));
        assertSame(session.getScreen(), manager.getForegroundSession().getScreen());
    }

    @Test
    void togglingBookmarkTwiceRemovesIt() {
        GuiBookmarkStore store = new GuiBookmarkStore();
        GuiSessionManager manager = new GuiSessionManager(new GuiHistoryStore(), store);
        GuiSession session = manager.registerOrReuseSession(new GuiScreen() {
        }, "Chat", testSource(2));

        manager.toggleBookmark(session.getId());
        manager.toggleBookmark(session.getId());

        assertFalse(manager.isSessionBookmarked(session.getId()));
    }

    @Test
    void bookmarkStoreRecordsTitle() {
        GuiBookmarkStore store = new GuiBookmarkStore();
        GuiSessionManager manager = new GuiSessionManager(new GuiHistoryStore(), store);
        GuiSession session = manager.registerOrReuseSession(new GuiScreen() {
        }, "My Chest", testSource(3));

        manager.toggleBookmark(session.getId());

        GuiSessionSourceKey key = GuiSessionSourceKey.fromSource(session.getSource());
        GuiBookmarkEntry entry = store.allBookmarks().get(key);
        assertEquals("My Chest", entry.getSessionTitle());
    }

    @Test
    void worldUnloadClearsBookmarks() {
        GuiBookmarkStore store = new GuiBookmarkStore();
        GuiSessionManager manager = new GuiSessionManager(new GuiHistoryStore(), store);
        GuiSession session = manager.registerOrReuseSession(new GuiScreen() {
        }, "Chat", testSource(4));
        manager.toggleBookmark(session.getId());

        manager.clearForWorldUnload();

        assertEquals(0, store.size());
    }

    @Test
    void directStoreOperationsWork() {
        GuiBookmarkStore store = new GuiBookmarkStore();
        GuiSessionSourceKey key = new GuiSessionSourceKey.BlockKey(100, 64, 200, 0);

        store.addBookmark(key, "Test", "TestClass");

        assertTrue(store.isBookmarked(key));
        assertEquals(1, store.size());

        store.removeBookmark(key);

        assertFalse(store.isBookmarked(key));
        assertEquals(0, store.size());
    }
}
