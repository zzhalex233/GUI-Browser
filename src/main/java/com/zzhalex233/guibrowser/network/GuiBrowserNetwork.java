package com.zzhalex233.guibrowser.network;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class GuiBrowserNetwork {
    private static final String CHANNEL_NAME = "guibrowser";
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);

    private GuiBrowserNetwork() {
    }

    public static void registerMessages() {
        CHANNEL.registerMessage(
            OpenRemoteBlockGuiMessage.Handler.class,
            OpenRemoteBlockGuiMessage.class,
            0,
            Side.SERVER
        );
    }

    public static void sendOpenRemoteBlock(int dimensionId, BlockPos pos, EnumFacing facing,
                                           EnumHand hand, float hitX, float hitY, float hitZ) {
        CHANNEL.sendToServer(new OpenRemoteBlockGuiMessage(dimensionId, pos, facing, hand, hitX, hitY, hitZ));
    }
}
