package com.zzhalex233.guibrowser.client.session;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionSourceTrackerTest {

    @Test
    void setPendingStoresSource() {
        InteractionSourceTracker tracker = new InteractionSourceTracker();
        GuiSessionSource source = new GuiSessionSource.BlockSource(new BlockPos(1, 2, 3), 0);

        tracker.setPending(source, 100L);
        GuiSessionSource result = tracker.consumePending(100L);

        assertEquals(source, result);
    }

    @Test
    void consumePendingClearsSource() {
        InteractionSourceTracker tracker = new InteractionSourceTracker();
        GuiSessionSource source = new GuiSessionSource.EntitySource(42);

        tracker.setPending(source, 100L);
        tracker.consumePending(100L);
        GuiSessionSource second = tracker.consumePending(100L);

        assertNull(second);
    }

    @Test
    void setPendingOverwritesPrevious() {
        InteractionSourceTracker tracker = new InteractionSourceTracker();
        GuiSessionSource a = new GuiSessionSource.BlockSource(new BlockPos(1, 2, 3), 0);
        GuiSessionSource b = new GuiSessionSource.EntitySource(99);

        tracker.setPending(a, 100L);
        tracker.setPending(b, 105L);
        GuiSessionSource result = tracker.consumePending(105L);

        assertEquals(b, result);
    }

    @Test
    void consumePendingReturnsNullWhenExpired() {
        InteractionSourceTracker tracker = new InteractionSourceTracker();
        GuiSessionSource source = new GuiSessionSource.BlockSource(new BlockPos(1, 2, 3), 0);

        tracker.setPending(source, 1000L);
        GuiSessionSource result = tracker.consumePending(3001L);

        assertNull(result);
    }

    @Test
    void consumePendingReturnsSourceWhenNotExpired() {
        InteractionSourceTracker tracker = new InteractionSourceTracker();
        GuiSessionSource source = new GuiSessionSource.BlockSource(new BlockPos(1, 2, 3), 0);

        tracker.setPending(source, 1000L);
        GuiSessionSource result = tracker.consumePending(3000L);

        assertEquals(source, result);
    }

    @Test
    void clearRemovesPending() {
        InteractionSourceTracker tracker = new InteractionSourceTracker();
        GuiSessionSource source = new GuiSessionSource.EntitySource(7);

        tracker.setPending(source, 100L);
        tracker.clear();
        GuiSessionSource result = tracker.consumePending(100L);

        assertNull(result);
    }

    @Test
    void hasPendingReturnsTrueWhenSetAndNotExpired() {
        InteractionSourceTracker tracker = new InteractionSourceTracker();
        GuiSessionSource source = new GuiSessionSource.BlockSource(new BlockPos(5, 10, 15), 0);

        assertFalse(tracker.hasPending(100L));

        tracker.setPending(source, 1000L);
        assertTrue(tracker.hasPending(1000L));
        assertTrue(tracker.hasPending(3000L));
        assertFalse(tracker.hasPending(3001L));

        tracker.consumePending(1000L);
        assertFalse(tracker.hasPending(1000L));
    }
}
