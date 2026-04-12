package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.chrome.GuiChromeOverlayController;
import com.zzhalex233.guibrowser.client.chrome.GuiChromeRenderer;
import com.zzhalex233.guibrowser.client.chrome.GuiChromeTarget;
import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.client.session.GuiSessionId;
import com.zzhalex233.guibrowser.client.session.GuiTrackingPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void guibrowser$drawChromeOverlay(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiScreen self = (GuiScreen) (Object) this;
        if (!GuiTrackingPolicy.shouldTrack(self)) {
            return;
        }
        GuiBrowserRuntime runtime = GuiBrowserRuntime.getInstance();
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        runtime.getChromeRenderer().render(resolution, mouseX, mouseY,
                Minecraft.getMinecraft().fontRenderer);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void guibrowser$interceptChromeClicks(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        GuiScreen self = (GuiScreen) (Object) this;
        if (!GuiTrackingPolicy.shouldTrack(self)) {
            return;
        }
        GuiBrowserRuntime runtime = GuiBrowserRuntime.getInstance();
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
                        controller.handleTabLeftClick(tabId);
                        GuiScreen targetScreen = runtime.getSessionManager().getSession(tabId).getScreen();
                        if (targetScreen != self) {
                            Minecraft.getMinecraft().displayGuiScreen(targetScreen);
                        }
                    }
                } else if (mouseButton == 2) {
                    GuiSessionId tabId = controller.getSessionIdForTabIndex(target.getTabIndex());
                    if (tabId != null) {
                        controller.handleTabMiddleClick(tabId);
                    }
                }
                break;
            case TAB_CLOSE:
                if (mouseButton == 0) {
                    GuiSessionId tabId = controller.getSessionIdForTabIndex(target.getTabIndex());
                    if (tabId != null) {
                        controller.handleTabMiddleClick(tabId);
                    }
                }
                break;
            case BOOKMARK_BUTTON:
                if (mouseButton == 0) {
                    controller.handleBookmarkButtonClick();
                }
                break;
            case HISTORY_BUTTON:
                if (mouseButton == 0) {
                    controller.handleHistoryButtonClick();
                }
                break;
            default:
                break;
        }
    }
}
