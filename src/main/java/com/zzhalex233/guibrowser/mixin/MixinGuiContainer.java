package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.chrome.GuiLayoutPolicy;
import com.zzhalex233.guibrowser.client.chrome.GuiLayoutState;
import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen {
    @Shadow
    public int guiTop;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void guibrowser$adjustGuiTopForChrome(CallbackInfo ci) {
        GuiScreen self = (GuiScreen) (Object) this;
        if (GuiBrowserRuntime.getInstance().getSessionManager().findSessionByScreen(self) == null) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        GuiLayoutState state = GuiLayoutPolicy.forScreen(self,
                resolution.getScaledWidth(), resolution.getScaledHeight());
        if (state.getMode() == GuiLayoutState.LayoutMode.PUSH_DOWN) {
            this.guiTop += state.getVerticalOffset();
        }
    }
}
