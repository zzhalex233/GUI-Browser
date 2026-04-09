package com.zzhalex233.guibrowser.client.event;

import com.zzhalex233.guibrowser.client.browser.BrowserManager;
import com.zzhalex233.guibrowser.client.browser.BrowserState;
import com.zzhalex233.guibrowser.client.gui.BrowserRootGui;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientForgeEventHandler {
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        BrowserManager manager = BrowserManager.getInstance();
        if (manager.getState() == BrowserState.CLOSED) {
            return;
        }
        if (!(event.getGui() instanceof BrowserRootGui)) {
            manager.closeBrowser();
        }
    }
}
