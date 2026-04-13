package net.minecraft.client.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

public class EntityPlayerSP extends EntityPlayer {
    public double getDistanceSq(double x, double y, double z) {
        return 0.0;
    }

    public double getDistanceSq(net.minecraft.entity.Entity entity) {
        return 0.0;
    }
}
