package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.chrome.GuiChromeLayout;
import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen {
    @Shadow
    public int guiTop;

    /**
     * Offset guiTop after initGui so that slot positions, hit-testing, and the
     * GL translate in drawScreen all account for the chrome bar.
     */
    @Inject(method = "initGui", at = @At("RETURN"))
    private void guibrowser$adjustGuiTopForChrome(CallbackInfo ci) {
        if (GuiBrowserRuntime.getInstance().getSessionManager().findSessionByScreen((GuiScreen) (Object) this) == null) {
            return;
        }
        this.guiTop += GuiChromeLayout.TOP_BAR_HEIGHT;
    }

    /**
     * Most vanilla GuiContainer subclasses recalculate (height-ySize)/2 inside
     * drawGuiContainerBackgroundLayer instead of using guiTop.  Push a GL
     * translate before that call so the background texture lands at the same
     * position as the slots.
     */
    @Inject(method = "drawScreen",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerBackgroundLayer(FII)V"))
    private void guibrowser$preBackgroundTranslate(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (GuiBrowserRuntime.getInstance().getSessionManager().findSessionByScreen((GuiScreen) (Object) this) == null) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, GuiChromeLayout.TOP_BAR_HEIGHT, 0);
    }

    @Inject(method = "drawScreen",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerBackgroundLayer(FII)V",
                     shift = At.Shift.AFTER))
    private void guibrowser$postBackgroundTranslate(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (GuiBrowserRuntime.getInstance().getSessionManager().findSessionByScreen((GuiScreen) (Object) this) == null) {
            return;
        }
        GlStateManager.popMatrix();
    }
}
