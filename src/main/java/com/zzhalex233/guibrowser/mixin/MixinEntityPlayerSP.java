package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

    @Inject(method = "closeScreen", at = @At("HEAD"), cancellable = true)
    private void guibrowser$suppressCloseForCachedContainer(CallbackInfo ci) {
        if (GuiBrowserRuntime.getInstance().isSuppressClosePacket()) {
            ci.cancel();
        }
    }
}
