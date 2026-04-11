package com.zzhalex233.guibrowser.proxy;

import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.config.BrowserConfig;
import com.zzhalex233.guibrowser.config.BrowserConfigLoader;
import net.minecraft.client.Minecraft;

import java.io.File;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        Minecraft minecraft = Minecraft.getMinecraft();
        File configFile = new File(new File(minecraft.gameDir, "config"), "guibrowser.cfg");
        BrowserConfig config = BrowserConfigLoader.load(configFile);
        GuiBrowserRuntime.initialize(config);
    }
}