package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @ModifyConstant(method = "processTryUseItemOnBlock", constant = @Constant(doubleValue = 64.0D))
    private double guibrowser$expandInteractionRange(double original) {
        if (GuiBrowserRuntime.getInstance().isBypassServerDistanceCheck()) {
            return Double.MAX_VALUE;
        }
        return original;
    }
}
