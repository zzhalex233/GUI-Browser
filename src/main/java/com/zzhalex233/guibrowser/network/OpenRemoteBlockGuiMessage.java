package com.zzhalex233.guibrowser.network;

import com.zzhalex233.guibrowser.common.RemoteGuiPlayer;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class OpenRemoteBlockGuiMessage implements IMessage {
    private int dimensionId;
    private int x;
    private int y;
    private int z;
    private int facingOrdinal;
    private int handOrdinal;
    private float hitX;
    private float hitY;
    private float hitZ;

    public OpenRemoteBlockGuiMessage() {
    }

    OpenRemoteBlockGuiMessage(int dimensionId, BlockPos pos, EnumFacing facing,
                              EnumHand hand, float hitX, float hitY, float hitZ) {
        this.dimensionId = dimensionId;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.facingOrdinal = facing.ordinal();
        this.handOrdinal = hand.ordinal();
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimensionId = buf.readInt();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        facingOrdinal = buf.readByte();
        handOrdinal = buf.readByte();
        hitX = buf.readFloat();
        hitY = buf.readFloat();
        hitZ = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimensionId);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeByte(facingOrdinal);
        buf.writeByte(handOrdinal);
        buf.writeFloat(hitX);
        buf.writeFloat(hitY);
        buf.writeFloat(hitZ);
    }

    private static EnumFacing readFacing(int ordinal) {
        EnumFacing[] values = EnumFacing.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return EnumFacing.UP;
        }
        return values[ordinal];
    }

    private static EnumHand readHand(int ordinal) {
        EnumHand[] values = EnumHand.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return EnumHand.MAIN_HAND;
        }
        return values[ordinal];
    }

    private static void handle(OpenRemoteBlockGuiMessage message, EntityPlayerMP player) {
        if (player == null || player.dimension != message.dimensionId) {
            return;
        }

        WorldServer world = player.getServerWorld();
        BlockPos pos = new BlockPos(message.x, message.y, message.z);
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock().isAir(state, world, pos)) {
            return;
        }

        EnumFacing facing = readFacing(message.facingOrdinal);
        EnumHand hand = readHand(message.handOrdinal);
        Container previousContainer = player.openContainer;
        RemoteGuiPlayer remoteGuiPlayer = (RemoteGuiPlayer) player;
        if (player.openContainer != player.inventoryContainer) {
            remoteGuiPlayer.guibrowser$allowNextCloseForRemoteTransition();
        }
        remoteGuiPlayer.guibrowser$setRemoteGuiInteraction();
        boolean openedContainer = false;
        try {
            Vec3d hitVec = RemoteBlockInteractionGeometry.selectForgeEventHitVec(
                pos,
                message.hitX,
                message.hitY,
                message.hitZ,
                ForgeHooks.rayTraceEyeHitVec(player,
                    player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue() + 1));
            PlayerInteractEvent.RightClickBlock event =
                ForgeHooks.onRightClickBlock(player, hand, pos, facing, hitVec);
            if (!event.isCanceled() && event.getUseBlock() != Event.Result.DENY) {
                state.getBlock().onBlockActivated(
                    world, pos, state, player, hand, facing, message.hitX, message.hitY, message.hitZ);
            }
            openedContainer = player.openContainer != previousContainer;
        } finally {
            if (!openedContainer) {
                remoteGuiPlayer.guibrowser$onCloseRemoteGui();
            }
        }
    }

    public static final class Handler implements IMessageHandler<OpenRemoteBlockGuiMessage, IMessage> {
        @Override
        public IMessage onMessage(OpenRemoteBlockGuiMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> handle(message, player));
            return null;
        }
    }
}
