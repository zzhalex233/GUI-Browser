package com.zzhalex233.guibrowser.network;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class RemoteBlockInteractionGeometry {
    private RemoteBlockInteractionGeometry() {
    }

    static Vec3d selectForgeEventHitVec(BlockPos remotePos, float hitX, float hitY, float hitZ,
                                        Vec3d playerReachHitVec) {
        return playerReachHitVec;
    }
}
