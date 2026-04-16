package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP {

    @Redirect(
        method = "onUpdate",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/inventory/Container;canInteractWith(Lnet/minecraft/entity/player/EntityPlayer;)Z")
    )
    private boolean guibrowser$bypassDistanceCheck(Container container, EntityPlayer player) {
        if (GuiBrowserRuntime.getInstance().getSessionManager().getForegroundSession() != null) {
            return true;
        }
        return container.canInteractWith(player);
    }
}
