package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

/**
 * Sends block interaction packets directly, bypassing PlayerControllerMP's
 * client-side reach distance check. Isolated into its own class so that
 * ContainerRestoreHandler can be loaded without eagerly resolving network classes.
 */
final class RemoteInteractionHelper {

    private RemoteInteractionHelper() {}

    static void sendBlockInteraction(EntityPlayerSP player, BlockPos pos) {
        player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(
            pos, EnumFacing.UP,
            EnumHand.MAIN_HAND,
            0.5f, 1.0f, 0.5f
        ));
        player.swingArm(EnumHand.MAIN_HAND);
    }
}
