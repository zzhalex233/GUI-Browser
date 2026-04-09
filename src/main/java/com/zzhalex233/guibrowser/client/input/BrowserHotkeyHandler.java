package com.zzhalex233.guibrowser.client.input;

import com.zzhalex233.guibrowser.client.browser.BrowserManager;
import com.zzhalex233.guibrowser.client.gui.BrowserRootGui;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class BrowserHotkeyHandler {
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || BrowserKeybinds.OPEN_BROWSER == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.world == null) {
            return;
        }

        while (BrowserKeybinds.OPEN_BROWSER.isPressed()) {
            toggleBrowser(minecraft, BrowserManager.getInstance());
        }
    }

    private static void toggleBrowser(Minecraft minecraft, BrowserManager manager) {
        if (manager.getState() == com.zzhalex233.guibrowser.client.browser.BrowserState.CLOSED) {
            manager.openEmptyBrowser();
            minecraft.displayGuiScreen(new BrowserRootGui(manager));
            return;
        }
        if (minecraft.currentScreen instanceof BrowserRootGui) {
            manager.closeBrowser();
            minecraft.displayGuiScreen(null);
            return;
        }
        manager.openEmptyBrowser();
        minecraft.displayGuiScreen(new BrowserRootGui(manager));
    }
}
