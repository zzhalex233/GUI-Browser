package net.minecraft.world;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class World {
    public boolean isRemote;

    public long getTotalWorldTime() {
        return 0L;
    }

    public boolean isAirBlock(BlockPos pos) {
        return false;
    }

    public Entity getEntityByID(int id) {
        return null;
    }
}
