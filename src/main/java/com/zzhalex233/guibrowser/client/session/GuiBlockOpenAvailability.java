package com.zzhalex233.guibrowser.client.session;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class GuiBlockOpenAvailability {

    private GuiBlockOpenAvailability() {
    }

    public enum Result {
        AVAILABLE,
        WRONG_DIMENSION,
        UNREACHABLE
    }

    public static Result check(@Nullable World world, int currentDimensionId, GuiSessionSourceKey.BlockKey key) {
        if (key.getDimensionId() != currentDimensionId) {
            return Result.WRONG_DIMENSION;
        }
        if (world == null) {
            return Result.UNREACHABLE;
        }

        BlockPos pos = new BlockPos(key.getX(), key.getY(), key.getZ());
        if (!world.isBlockLoaded(pos) || world.isAirBlock(pos)) {
            return Result.UNREACHABLE;
        }
        return Result.AVAILABLE;
    }
}
