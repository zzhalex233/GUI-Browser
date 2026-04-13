package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.client.session.ContainerRestoreHandler;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketOpenWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Inject(method = "handleOpenWindow", at = @At("HEAD"), cancellable = true)
    private void guibrowser$interceptOpenWindowForRestore(SPacketOpenWindow packetIn, CallbackInfo ci) {
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            return;
        }

        ContainerRestoreHandler restoreHandler = GuiBrowserRuntime.getInstance().getRestoreHandler();
        if (restoreHandler == null || !restoreHandler.isPendingRestore()) {
            return;
        }

        GuiSession session = restoreHandler.getPendingRestoreSession();
        if (session == null) {
            return;
        }

        if (session.getScreen() instanceof GuiContainer) {
            GuiContainer cachedScreen = (GuiContainer) session.getScreen();
            cachedScreen.inventorySlots.windowId = packetIn.getWindowId();
            session.clearStale();
            restoreHandler.clearPendingRestore();
            Minecraft.getMinecraft().displayGuiScreen(cachedScreen);
            ci.cancel();
        }
    }
}
