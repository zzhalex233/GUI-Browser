package com.zzhalex233.guibrowser.client.session;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiSessionSourceTest {

    @Test
    void blockSourcesWithSamePosAndDimensionAreEqual() {
        BlockPos pos = new BlockPos(10, 64, 20);
        GuiSessionSource.BlockSource a = new GuiSessionSource.BlockSource(pos, 0);
        GuiSessionSource.BlockSource b = new GuiSessionSource.BlockSource(new BlockPos(10, 64, 20), 0);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void blockSourcesWithDifferentPosAreNotEqual() {
        GuiSessionSource.BlockSource a = new GuiSessionSource.BlockSource(new BlockPos(10, 64, 20), 0);
        GuiSessionSource.BlockSource b = new GuiSessionSource.BlockSource(new BlockPos(99, 64, 20), 0);

        assertNotEquals(a, b);
    }

    @Test
    void blockSourcesWithDifferentDimensionAreNotEqual() {
        BlockPos pos = new BlockPos(10, 64, 20);
        GuiSessionSource.BlockSource a = new GuiSessionSource.BlockSource(pos, 0);
        GuiSessionSource.BlockSource b = new GuiSessionSource.BlockSource(new BlockPos(10, 64, 20), -1);

        assertNotEquals(a, b);
    }

    @Test
    void entitySourcesWithSameIdAreEqual() {
        GuiSessionSource.EntitySource a = new GuiSessionSource.EntitySource(42);
        GuiSessionSource.EntitySource b = new GuiSessionSource.EntitySource(42);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void entitySourcesWithDifferentIdAreNotEqual() {
        GuiSessionSource.EntitySource a = new GuiSessionSource.EntitySource(42);
        GuiSessionSource.EntitySource b = new GuiSessionSource.EntitySource(99);

        assertNotEquals(a, b);
    }

    @Test
    void blockSourceAndEntitySourceAreNeverEqual() {
        GuiSessionSource.BlockSource block = new GuiSessionSource.BlockSource(new BlockPos(0, 0, 0), 0);
        GuiSessionSource.EntitySource entity = new GuiSessionSource.EntitySource(0);

        assertNotEquals(block, entity);
        assertNotEquals(entity, block);
    }

    @Test
    void toStringContainsRelevantInfo() {
        GuiSessionSource.BlockSource block = new GuiSessionSource.BlockSource(new BlockPos(10, 64, 20), -1);
        String blockStr = block.toString();
        assertTrue(blockStr.contains("10"), "should contain x coordinate");
        assertTrue(blockStr.contains("64"), "should contain y coordinate");
        assertTrue(blockStr.contains("20"), "should contain z coordinate");
        assertTrue(blockStr.contains("-1"), "should contain dimension id");

        GuiSessionSource.EntitySource entity = new GuiSessionSource.EntitySource(42);
        String entityStr = entity.toString();
        assertTrue(entityStr.contains("42"), "should contain entity id");
    }
}
