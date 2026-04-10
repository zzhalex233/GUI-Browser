package com.zzhalex233.guibrowser.client.input;

import com.zzhalex233.guibrowser.client.browser.BrowserShellController;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class BrowserHotkeyHandler {
    private final BrowserShellController controller;

    public BrowserHotkeyHandler(BrowserShellController controller) {
        this.controller = controller;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        controller.flushDeferredUiActions();
        controller.tickCaptureRequest();
        if (BrowserKeybinds.CAPTURE_GUI == null) {
            return;
        }

        while (BrowserKeybinds.CAPTURE_GUI.isPressed()) {
            controller.requestHotkeyCapture();
        }
    }
}