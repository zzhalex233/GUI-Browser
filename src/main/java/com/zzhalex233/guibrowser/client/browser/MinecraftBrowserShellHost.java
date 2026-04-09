package com.zzhalex233.guibrowser.client.browser;

import com.zzhalex233.guibrowser.client.gui.BrowserRootGui;
import net.minecraft.client.Minecraft;

public final class MinecraftBrowserShellHost implements BrowserShellHost {
    @Override
    public boolean isInWorld() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.player != null && minecraft.world != null;
    }

    @Override
    public boolean isBrowserRootActive() {
        return Minecraft.getMinecraft().currentScreen instanceof BrowserRootGui;
    }

    @Override
    public void showBrowserRoot(BrowserShellController controller) {
        Minecraft.getMinecraft().displayGuiScreen(new BrowserRootGui(controller));
    }

    @Override
    public void closeCurrentScreen() {
        Minecraft.getMinecraft().displayGuiScreen(null);
    }
}
