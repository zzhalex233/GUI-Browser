package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.chrome.GuiChromeOverlayController;
import com.zzhalex233.guibrowser.client.chrome.GuiChromeOverlayController.TabSwitchResult;
import com.zzhalex233.guibrowser.client.chrome.GuiChromeRenderer;
import com.zzhalex233.guibrowser.client.chrome.GuiChromeTarget;
import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionId;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void guibrowser$pruneInvalidSessions(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiScreen self = (GuiScreen) (Object) this;
        GuiBrowserRuntime runtime = GuiBrowserRuntime.getInstance();
        Minecraft mc = Minecraft.getMinecraft();
        int currentDimensionId = mc.player == null ? Integer.MIN_VALUE : mc.player.dimension;
        boolean closedForeground = runtime.getSourceValidator().pruneInvalidSessions(mc.world, currentDimensionId, self);
        if (closedForeground && runtime.getSessionManager().findSessionByScreen(self) == null) {
            mc.displayGuiScreen(null);
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void guibrowser$drawChromeOverlay(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiScreen self = (GuiScreen) (Object) this;
        GuiBrowserRuntime runtime = GuiBrowserRuntime.getInstance();
        if (runtime.getSessionManager().findSessionByScreen(self) == null) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        runtime.getChromeRenderer().render(resolution, mouseX, mouseY,
                Minecraft.getMinecraft().fontRenderer);
        runtime.getRestoreHandler().tickPendingRestore();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void guibrowser$interceptChromeClicks(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        GuiScreen self = (GuiScreen) (Object) this;
        GuiBrowserRuntime runtime = GuiBrowserRuntime.getInstance();
        if (runtime.getSessionManager().findSessionByScreen(self) == null) {
            return;
        }
        GuiChromeOverlayController controller = runtime.getChromeController();
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();

        GuiChromeTarget target = controller.resolveTarget(mouseX, mouseY, screenWidth, screenHeight);

        if (target.getType() == GuiChromeTarget.Type.NONE) {
            return;
        }

        ci.cancel();

        switch (target.getType()) {
            case TAB:
                if (mouseButton == 0) {
                    GuiSessionId tabId = controller.getSessionIdForTabIndex(target.getTabIndex());
                    if (tabId != null) {
                        TabSwitchResult result = controller.handleTabLeftClick(tabId);
                        if (result == TabSwitchResult.DIRECT_SWITCH) {
                            GuiScreen targetScreen = runtime.getSessionManager().getSession(tabId).getScreen();
                            if (targetScreen != self) {
                                runtime.setSuppressClosePacket(true);
                                Minecraft.getMinecraft().displayGuiScreen(targetScreen);
                                runtime.setSuppressClosePacket(false);
                            }
                        }
                    }
                } else if (mouseButton == 2) {
                    GuiSessionId tabId = controller.getSessionIdForTabIndex(target.getTabIndex());
                    if (tabId != null) {
                        GuiSession closedSession = runtime.getSessionManager().findSession(tabId);
                        boolean isCurrentScreen = closedSession != null && closedSession.getScreen() == self;
                        controller.handleTabMiddleClick(tabId);
                        if (isCurrentScreen) {
                            Minecraft.getMinecraft().displayGuiScreen(null);
                        }
                    }
                }
                break;
            case TAB_CLOSE:
                if (mouseButton == 0) {
                    GuiSessionId tabId = controller.getSessionIdForTabIndex(target.getTabIndex());
                    if (tabId != null) {
                        GuiSession closedSession = runtime.getSessionManager().findSession(tabId);
                        boolean isCurrentScreen = closedSession != null && closedSession.getScreen() == self;
                        controller.handleTabMiddleClick(tabId);
                        if (isCurrentScreen) {
                            Minecraft.getMinecraft().displayGuiScreen(null);
                        }
                    }
                }
                break;
            case BOOKMARK_BUTTON:
                if (mouseButton == 0) {
                    controller.handleBookmarkButtonClick(self);
                } else if (mouseButton == 1) {
                    controller.handleBookmarkButtonRightClick(self);
                }
                break;
            case HISTORY_BUTTON:
                if (mouseButton == 0) {
                    controller.handleHistoryButtonClick(self);
                }
                break;
            default:
                break;
        }
    }
}
