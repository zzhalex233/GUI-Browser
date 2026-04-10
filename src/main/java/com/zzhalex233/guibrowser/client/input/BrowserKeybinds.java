package com.zzhalex233.guibrowser.client.input;

import com.zzhalex233.guibrowser.client.browser.BrowserManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public final class BrowserKeybinds {
    public static KeyBinding CAPTURE_GUI;

    private static boolean registered;

    private BrowserKeybinds() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        CAPTURE_GUI = new KeyBinding(
            "key.guibrowser.capture_gui",
            BrowserManager.getInstance().getConfig().getCaptureHotkeyKeyCode(),
            "key.categories.guibrowser"
        );
        ClientRegistry.registerKeyBinding(CAPTURE_GUI);
        registered = true;
    }
}