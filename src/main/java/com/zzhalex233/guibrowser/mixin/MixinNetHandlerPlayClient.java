package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketCloseWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    /**
     * When the server closes a container window, mark any cached session
     * that holds a GuiContainer with the matching windowId as stale.
     */
    @Inject(method = "handleCloseWindow", at = @At("HEAD"))
    private void guibrowser$markStaleOnCloseWindow(SPacketCloseWindow packetIn, CallbackInfo ci) {
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            return;
        }
        int closedWindowId = ((AccessorSPacketCloseWindow) packetIn).guibrowser$getWindowId();
        GuiSessionManager sessionManager = GuiBrowserRuntime.getInstance().getSessionManager();
        for (GuiSession session : sessionManager.listAllSessions()) {
            if (session.getScreen() instanceof GuiContainer) {
                GuiContainer container = (GuiContainer) session.getScreen();
                if (container.inventorySlots.windowId == closedWindowId) {
                    session.markStale();
                    if (session.getId().equals(sessionManager.getLastServerWindowSessionId())) {
                        sessionManager.setLastServerWindowSessionId(null);
                    }
                    if (session.isForeground()) {
                        Minecraft mc = Minecraft.getMinecraft();
                        if (mc.currentScreen == session.getScreen()) {
                            mc.displayGuiScreen(null);
                        }
                    }
                }
            }
        }
    }
}
