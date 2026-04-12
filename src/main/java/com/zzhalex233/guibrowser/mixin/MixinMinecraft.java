package com.zzhalex233.guibrowser.mixin;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.client.session.GuiLifecycleBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow
    @Nullable
    public GuiScreen currentScreen;

    @Unique
    private GuiLifecycleBridge.TransitionDecision guibrowser$transitionDecision;

    @Inject(
        method = "displayGuiScreen",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;onGuiClosed()V", shift = At.Shift.BEFORE),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void guibrowser$beforeDisplay(@Nullable GuiScreen guiScreenIn, CallbackInfo callbackInfo, GuiScreen current, GuiOpenEvent event) {
        guibrowser$transitionDecision = GuiBrowserRuntime.getInstance().getLifecycleBridge().onBeforeDisplay(current, guiScreenIn, false);
    }

    @Redirect(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;onGuiClosed()V"))
    private void guibrowser$maybeCloseCurrentScreen(GuiScreen screen) {
        if (guibrowser$transitionDecision != null && guibrowser$transitionDecision.shouldSuppressCurrentClose()) {
            return;
        }
        screen.onGuiClosed();
    }

    @Inject(method = "displayGuiScreen", at = @At("RETURN"))
    private void guibrowser$afterDisplay(@Nullable GuiScreen guiScreenIn, CallbackInfo callbackInfo) {
        GuiBrowserRuntime.getInstance().getLifecycleBridge().onAfterDisplay(currentScreen);
        guibrowser$transitionDecision = null;
    }
}
