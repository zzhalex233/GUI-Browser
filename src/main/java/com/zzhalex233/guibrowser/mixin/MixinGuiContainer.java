package com.zzhalex233.guibrowser.mixin;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Placeholder mixin for GuiContainer. The chrome bar is rendered as a pure
 * overlay on top of the container — no guiTop offset, no GL translate.
 * Container subclasses keep their original coordinate calculations untouched.
 */
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen {
}
