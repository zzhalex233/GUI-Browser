package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiSessionSourceValidatorTest {

    @Test
    void destroysBlockSessionWhenSourceBlockTurnsIntoAir() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession session = manager.registerOrReuseSession(
            new GuiScreen() {
            },
            "Workbench",
            new GuiSessionSource.BlockSource(new BlockPos(10, 64, 10), 0)
        );
        manager.setLastServerWindowSessionId(session.getId());

        GuiSessionSourceValidator validator = new GuiSessionSourceValidator(manager);
        FakeWorld world = new FakeWorld();
        world.setAir(new BlockPos(10, 64, 10), true);

        boolean closedForeground = validator.pruneInvalidSessions(world, 0, session.getScreen());

        assertTrue(closedForeground);
        assertNull(manager.findSession(session.getId()));
        assertNull(manager.getLastServerWindowSessionId());
    }

    @Test
    void destroysEntitySessionWhenSourceEntityDisappears() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession session = manager.registerOrReuseSession(
            new GuiScreen() {
            },
            "Merchant",
            new GuiSessionSource.EntitySource(42)
        );

        GuiSessionSourceValidator validator = new GuiSessionSourceValidator(manager);

        boolean closedForeground = validator.pruneInvalidSessions(new FakeWorld(), 0, session.getScreen());

        assertTrue(closedForeground);
        assertNull(manager.findSession(session.getId()));
    }

    @Test
    void keepsSessionWhenSourceStillExists() {
        GuiSessionManager manager = new GuiSessionManager();
        GuiSession session = manager.registerOrReuseSession(
            new GuiScreen() {
            },
            "Chest",
            new GuiSessionSource.BlockSource(new BlockPos(2, 64, 2), 0)
        );

        GuiSessionSourceValidator validator = new GuiSessionSourceValidator(manager);
        FakeWorld world = new FakeWorld();
        world.setAir(new BlockPos(2, 64, 2), false);

        boolean closedForeground = validator.pruneInvalidSessions(world, 0, session.getScreen());

        assertFalse(closedForeground);
        assertTrue(manager.findSession(session.getId()) != null);
    }

    private static final class FakeWorld extends WorldClient {
        private final Map<BlockPos, Boolean> airByPos = new HashMap<>();
        private final Map<Integer, Entity> entities = new HashMap<>();

        void setAir(BlockPos pos, boolean air) {
            airByPos.put(pos, air);
        }

        void addEntity(int id, Entity entity) {
            entities.put(id, entity);
        }

        @Override
        public boolean isAirBlock(BlockPos pos) {
            Boolean air = airByPos.get(pos);
            return air != null && air;
        }

        @Override
        public Entity getEntityByID(int id) {
            return entities.get(id);
        }
    }
}
