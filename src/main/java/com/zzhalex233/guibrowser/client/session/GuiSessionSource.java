package com.zzhalex233.guibrowser.client.session;

import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public abstract class GuiSessionSource {

    private GuiSessionSource() {
    }

    public static final class BlockSource extends GuiSessionSource {
        private final BlockPos pos;
        private final int dimensionId;

        public BlockSource(BlockPos pos, int dimensionId) {
            this.pos = Objects.requireNonNull(pos, "pos");
            this.dimensionId = dimensionId;
        }

        public BlockPos getPos() {
            return pos;
        }

        public int getDimensionId() {
            return dimensionId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof BlockSource)) {
                return false;
            }
            BlockSource that = (BlockSource) other;
            return dimensionId == that.dimensionId && pos.equals(that.pos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, dimensionId);
        }

        @Override
        public String toString() {
            return "BlockSource{pos=" + pos + ", dimensionId=" + dimensionId + "}";
        }
    }

    public static final class EntitySource extends GuiSessionSource {
        private final int entityId;

        public EntitySource(int entityId) {
            this.entityId = entityId;
        }

        public int getEntityId() {
            return entityId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EntitySource)) {
                return false;
            }
            EntitySource that = (EntitySource) other;
            return entityId == that.entityId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityId);
        }

        @Override
        public String toString() {
            return "EntitySource{entityId=" + entityId + "}";
        }
    }
}
