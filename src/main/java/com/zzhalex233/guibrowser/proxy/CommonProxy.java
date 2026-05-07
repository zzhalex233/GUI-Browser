package com.zzhalex233.guibrowser.proxy;

import com.zzhalex233.guibrowser.network.GuiBrowserNetwork;

public class CommonProxy implements IProxy {
    @Override
    public void preInit() {
        GuiBrowserNetwork.registerMessages();
    }
}
