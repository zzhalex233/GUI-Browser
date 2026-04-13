package net.minecraft.client.multiplayer;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PlayerControllerMP {
    public net.minecraft.util.EnumActionResult processRightClickBlock(
            EntityPlayerSP player, WorldClient world, BlockPos pos,
            EnumFacing facing, Vec3d hitVec, EnumHand hand) {
        return null;
    }

    public net.minecraft.util.EnumActionResult interactWithEntity(
            EntityPlayer player, Entity target, EnumHand hand) {
        return null;
    }
}
