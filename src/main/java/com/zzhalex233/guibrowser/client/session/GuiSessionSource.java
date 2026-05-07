package com.zzhalex233.guibrowser.client.session;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public abstract class GuiSessionSource {

    private GuiSessionSource() {
    }

    public static final class BlockSource extends GuiSessionSource {
        private final BlockPos pos;
        private final int dimensionId;
        private final EnumFacing facing;
        private final float hitX;
        private final float hitY;
        private final float hitZ;

        public BlockSource(BlockPos pos, int dimensionId) {
            this(pos, dimensionId, EnumFacing.UP, 0.5f, 1.0f, 0.5f);
        }

        public BlockSource(BlockPos pos, int dimensionId, EnumFacing facing, float hitX, float hitY, float hitZ) {
            this.pos = Objects.requireNonNull(pos, "pos");
            this.dimensionId = dimensionId;
            this.facing = Objects.requireNonNull(facing, "facing");
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitZ = hitZ;
        }

        public BlockPos getPos() {
            return pos;
        }

        public int getDimensionId() {
            return dimensionId;
        }

        public EnumFacing getFacing() {
            return facing;
        }

        public float getHitX() {
            return hitX;
        }

        public float getHitY() {
            return hitY;
        }

        public float getHitZ() {
            return hitZ;
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
            return "BlockSource{pos=" + pos
                + ", dimensionId=" + dimensionId
                + ", facing=" + facing
                + ", hitX=" + hitX
                + ", hitY=" + hitY
                + ", hitZ=" + hitZ
                + "}";
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
