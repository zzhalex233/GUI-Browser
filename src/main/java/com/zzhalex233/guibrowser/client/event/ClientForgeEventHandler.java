package com.zzhalex233.guibrowser.client.event;

import com.zzhalex233.guibrowser.client.persistence.TabPersistenceManager;
import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.client.session.GuiLifecycleBridge;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import com.zzhalex233.guibrowser.client.session.InteractionSourceTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import javax.annotation.Nullable;

public final class ClientForgeEventHandler {
    private final GuiLifecycleBridge lifecycleBridge;
    private final InteractionSourceTracker sourceTracker;
    @Nullable
    private final GuiSessionManager sessionManager;

    public ClientForgeEventHandler(GuiLifecycleBridge lifecycleBridge, InteractionSourceTracker sourceTracker) {
        this(lifecycleBridge, sourceTracker, null);
    }

    public ClientForgeEventHandler(GuiLifecycleBridge lifecycleBridge, InteractionSourceTracker sourceTracker,
                                   @Nullable GuiSessionManager sessionManager) {
        this.lifecycleBridge = lifecycleBridge;
        this.sourceTracker = sourceTracker;
        this.sessionManager = sessionManager;
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
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            GuiBrowserRuntime runtime = GuiBrowserRuntime.getInstance();
            String worldId = deriveWorldId(mc);
            if (worldId != null) {
                runtime.updateDataDirForWorld(worldId);
            }
            if (sessionManager != null && runtime.getDataDir() != null) {
                TabPersistenceManager.restoreAsStale(sessionManager, runtime.getDataDir());
            }
        });
    }

    private static String deriveWorldId(Minecraft mc) {
        IntegratedServer server = mc.getIntegratedServer();
        if (server != null) {
            return sanitizeDirName(server.getFolderName());
        }
        ServerData serverData = mc.getCurrentServerData();
        if (serverData != null && serverData.serverIP != null) {
            return sanitizeDirName(serverData.serverIP);
        }
        return null;
    }

    private static String sanitizeDirName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getWorld().isRemote) return;
        BlockPos pos = event.getPos();
        EnumFacing facing = event.getFace() == null ? EnumFacing.UP : event.getFace();
        Vec3d hitVec = event.getHitVec();
        float hitX = 0.5f;
        float hitY = 1.0f;
        float hitZ = 0.5f;
        if (hitVec != null) {
            hitX = (float) (hitVec.x - pos.getX());
            hitY = (float) (hitVec.y - pos.getY());
            hitZ = (float) (hitVec.z - pos.getZ());
        }
        sourceTracker.setPending(
            new GuiSessionSource.BlockSource(pos, event.getEntityPlayer().dimension, facing, hitX, hitY, hitZ),
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
