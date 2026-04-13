package com.zzhalex233.guibrowser.client.event;

import com.zzhalex233.guibrowser.client.session.GuiLifecycleBridge;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import com.zzhalex233.guibrowser.client.session.InteractionSourceTracker;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public final class ClientForgeEventHandler {
    private final GuiLifecycleBridge lifecycleBridge;
    private final InteractionSourceTracker sourceTracker;

    public ClientForgeEventHandler(GuiLifecycleBridge lifecycleBridge, InteractionSourceTracker sourceTracker) {
        this.lifecycleBridge = lifecycleBridge;
        this.sourceTracker = sourceTracker;
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

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getWorld().isRemote) return;
        sourceTracker.setPending(
            new GuiSessionSource.BlockSource(event.getPos(), event.getEntityPlayer().dimension),
            System.currentTimeMillis()
        );
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getWorld().isRemote) return;
        sourceTracker.setPending(
            new GuiSessionSource.EntitySource(event.getTarget().getEntityId()),
            System.currentTimeMillis()
        );
    }
}
