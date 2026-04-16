package com.zzhalex233.guibrowser.mixin;

import net.minecraft.network.play.server.SPacketCloseWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SPacketCloseWindow.class)
public interface AccessorSPacketCloseWindow {
    @Accessor("windowId")
    int guibrowser$getWindowId();
}
