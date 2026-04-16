package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.client.session.ContainerRestoreHandler;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketCloseWindow;
import net.minecraft.network.play.server.SPacketOpenWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * When the server closes a container window, destroy any cached session
     * that holds a GuiContainer with the matching windowId.
     * This prevents item duplication via stale cached containers.
     */
    @Inject(method = "handleCloseWindow", at = @At("HEAD"))
    private void guibrowser$destroySessionOnCloseWindow(SPacketCloseWindow packetIn, CallbackInfo ci) {
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            return;
        }
        int closedWindowId = ((AccessorSPacketCloseWindow) packetIn).guibrowser$getWindowId();
        GuiSessionManager sessionManager = GuiBrowserRuntime.getInstance().getSessionManager();
        List<GuiSession> toDestroy = new ArrayList<>();
        for (GuiSession session : sessionManager.listAllSessions()) {
            if (session.getScreen() instanceof GuiContainer) {
                GuiContainer container = (GuiContainer) session.getScreen();
                if (container.inventorySlots.windowId == closedWindowId) {
                    toDestroy.add(session);
                }
            }
        }
        for (GuiSession session : toDestroy) {
            boolean wasForeground = session.isForeground();
            sessionManager.destroySession(session.getId());
            if (wasForeground) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen == session.getScreen()) {
                    mc.displayGuiScreen(null);
                }
            }
        }
    }
}
