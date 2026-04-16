package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @Redirect(
        method = "processTryUseItemOnBlock",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/entity/player/EntityPlayerMP;getDistanceSq(DDD)D")
    )
    private double guibrowser$bypassDistanceCheck(EntityPlayerMP player, double x, double y, double z) {
        if (GuiBrowserRuntime.getInstance().isBypassServerDistanceCheck()) {
            return 0.0D;
        }
        return player.getDistanceSq(x, y, z);
    }
}
