package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @Unique
    private static final Logger LOGGER = LogManager.getLogger("GuiBrowser/DistanceBypass");

    @Redirect(
        method = "processTryUseItemOnBlock",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/entity/player/EntityPlayerMP;getDistanceSq(DDD)D")
    )
    private double guibrowser$bypassDistanceCheck(EntityPlayerMP player, double x, double y, double z) {
        boolean bypass = GuiBrowserRuntime.getInstance().isBypassServerDistanceCheck();
        double realDist = player.getDistanceSq(x, y, z);
        LOGGER.info("[GuiBrowser] Distance check: bypass={}, realDist={}", bypass, realDist);
        if (bypass) {
            return 0.0D;
        }
        return realDist;
    }
}
