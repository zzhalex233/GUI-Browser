package com.zzhalex233.guibrowser.client.event;

import com.zzhalex233.guibrowser.client.session.GuiLifecycleBridge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public final class ClientForgeEventHandler {
    private final GuiLifecycleBridge lifecycleBridge;

    public ClientForgeEventHandler(GuiLifecycleBridge lifecycleBridge) {
        this.lifecycleBridge = lifecycleBridge;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            lifecycleBridge.onWorldUnload();
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        lifecycleBridge.onWorldUnload();
    }
}
