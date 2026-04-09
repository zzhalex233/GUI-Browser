package com.zzhalex233.guibrowser.client.event;

import com.zzhalex233.guibrowser.client.browser.BrowserShellController;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientForgeEventHandler {
    private final BrowserShellController controller;

    public ClientForgeEventHandler(BrowserShellController controller) {
        this.controller = controller;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        controller.handleExternalGuiOpen(event.getGui());
    }
}
