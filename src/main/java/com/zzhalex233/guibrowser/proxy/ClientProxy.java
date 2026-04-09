package com.zzhalex233.guibrowser.proxy;

import com.zzhalex233.guibrowser.client.browser.BrowserManager;
import com.zzhalex233.guibrowser.client.browser.BrowserShellController;
import com.zzhalex233.guibrowser.client.browser.MinecraftBrowserShellHost;
import com.zzhalex233.guibrowser.client.command.CommandGuiBrowser;
import com.zzhalex233.guibrowser.client.event.ClientForgeEventHandler;
import com.zzhalex233.guibrowser.client.input.BrowserHotkeyHandler;
import com.zzhalex233.guibrowser.client.input.BrowserKeybinds;
import com.zzhalex233.guibrowser.config.BrowserConfig;
import com.zzhalex233.guibrowser.config.BrowserConfigLoader;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import java.io.File;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        Minecraft minecraft = Minecraft.getMinecraft();
        File configFile = new File(new File(minecraft.gameDir, "config"), "guibrowser.cfg");
        BrowserConfig config = BrowserConfigLoader.load(configFile);
        BrowserManager.initialize(config);
        BrowserShellController controller = new BrowserShellController(BrowserManager.getInstance(), new MinecraftBrowserShellHost());
        BrowserKeybinds.register();
        MinecraftForge.EVENT_BUS.register(new BrowserHotkeyHandler(controller));
        MinecraftForge.EVENT_BUS.register(new ClientForgeEventHandler(controller));
        ClientCommandHandler.instance.registerCommand(new CommandGuiBrowser(controller));
    }
}
