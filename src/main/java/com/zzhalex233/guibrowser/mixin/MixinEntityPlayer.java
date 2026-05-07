package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.common.RemoteGuiPlayer;
import com.zzhalex233.guibrowser.common.RemoteGuiPlayerState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer implements RemoteGuiPlayer {
    @Unique
    private final RemoteGuiPlayerState guibrowser$remoteGuiState = new RemoteGuiPlayerState();

    @Override
    public void guibrowser$setRemoteGuiInteraction() {
        guibrowser$remoteGuiState.setRemoteGuiInteraction();
    }

    @Override
    public void guibrowser$allowNextCloseForRemoteTransition() {
        guibrowser$remoteGuiState.allowNextCloseForRemoteTransition();
    }

    @Override
    public void guibrowser$onCloseRemoteGui() {
        guibrowser$remoteGuiState.onCloseRemoteGui();
    }

    @Override
    public boolean guibrowser$canInteractWithRemoteGui() {
        return guibrowser$remoteGuiState.canInteractWithRemoteGui();
    }

    @Redirect(
        method = "onUpdate",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/inventory/Container;canInteractWith(Lnet/minecraft/entity/player/EntityPlayer;)Z")
    )
    private boolean guibrowser$allowRemoteGuiInteraction(Container container, EntityPlayer player) {
        return container.canInteractWith(player) || guibrowser$canInteractWithRemoteGui();
    }

    @Inject(method = "closeScreen", at = @At("HEAD"))
    private void guibrowser$handleCloseScreen(CallbackInfo ci) {
        guibrowser$onCloseRemoteGui();
    }
}
