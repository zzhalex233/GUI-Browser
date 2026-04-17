package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketCloseWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Inject(
        method = "handleCloseWindow",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;closeScreenAndDropStack()V"),
        cancellable = true
    )
    private void guibrowser$keepTrackedContainerTabsAlive(SPacketCloseWindow packetIn, CallbackInfo ci) {
        int closedWindowId = ((AccessorSPacketCloseWindow) packetIn).guibrowser$getWindowId();
        GuiSessionManager sessionManager = GuiBrowserRuntime.getInstance().getSessionManager();
        for (GuiSession session : sessionManager.listAllSessions()) {
            if (!(session.getScreen() instanceof GuiContainer)) {
                continue;
            }

            GuiContainer container = (GuiContainer) session.getScreen();
            if (container.inventorySlots.windowId != closedWindowId) {
                continue;
            }

            if (session.getId().equals(sessionManager.getLastServerWindowSessionId())) {
                sessionManager.setLastServerWindowSessionId(null);
            }
            ci.cancel();
            return;
        }
    }
}
