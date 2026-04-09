package com.zzhalex233.guibrowser.client.input;

import com.zzhalex233.guibrowser.client.browser.BrowserManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public final class BrowserKeybinds {
    public static KeyBinding OPEN_BROWSER;

    private static boolean registered;

    private BrowserKeybinds() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        OPEN_BROWSER = new KeyBinding(
            "key.guibrowser.open_browser",
            BrowserManager.getInstance().getConfig().getOpenBrowserKeyCode(),
            "key.categories.guibrowser"
        );
        ClientRegistry.registerKeyBinding(OPEN_BROWSER);
        registered = true;
    }
}
