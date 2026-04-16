package com.zzhalex233.guibrowser.client.session;

import com.zzhalex233.guibrowser.config.ContainerCacheMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class ContainerRestoreHandler {

    private static final long RESTORE_TIMEOUT_MS = 5000L;

    private final ContainerCacheMode cacheMode;
    @Nullable
    private GuiSession pendingRestoreSession;
    private long pendingRestoreTimestamp;

    public ContainerRestoreHandler(ContainerCacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    /**
     * Attempt to restore a stale tab by re-interacting with its source.
     * Returns true if the interaction was initiated, false if the source is unreachable.
     */
    public boolean requestRestore(GuiSession session) {
        if (!session.isStale()) {
            return false; // Not stale, nothing to restore
        }
        GuiSessionSource source = session.getSource();
        if (source == null) {
            return false; // No source, can't restore
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null) {
            return false;
        }

        if (source instanceof GuiSessionSource.BlockSource) {
            return restoreBlock((GuiSessionSource.BlockSource) source, session, mc, player);
        } else if (source instanceof GuiSessionSource.EntitySource) {
            return restoreEntity((GuiSessionSource.EntitySource) source, session, mc, player);
        }
        return false;
    }

    private boolean restoreBlock(GuiSessionSource.BlockSource blockSource, GuiSession session,
                                  Minecraft mc, EntityPlayerSP player) {
        BlockPos pos = blockSource.getPos();
        WorldClient world = mc.world;
        if (world == null) {
            return false;
        }

        // Check dimension matches
        if (player.dimension != blockSource.getDimensionId()) {
            return false;
        }

        // For HYBRID mode, set pending restore so MixinNetHandlerPlayClient can intercept
        if (cacheMode == ContainerCacheMode.HYBRID) {
            pendingRestoreSession = session;
            pendingRestoreTimestamp = System.currentTimeMillis();
        }

        // Send the interaction packet directly to bypass client-side reach check.
        // Deferred to RemoteInteractionHelper to avoid loading network classes eagerly.
        RemoteInteractionHelper.sendBlockInteraction(player, pos);
        return true;
    }

    private boolean restoreEntity(GuiSessionSource.EntitySource entitySource, GuiSession session,
                                   Minecraft mc, EntityPlayerSP player) {
        World world = mc.world;
        if (world == null) {
            return false;
        }

        Entity entity = world.getEntityByID(entitySource.getEntityId());
        if (entity == null) {
            return false; // Entity no longer exists
        }

        // For HYBRID mode, set pending restore
        if (cacheMode == ContainerCacheMode.HYBRID) {
            pendingRestoreSession = session;
            pendingRestoreTimestamp = System.currentTimeMillis();
        }

        // Simulate interaction with entity
        mc.playerController.interactWithEntity(player, entity, EnumHand.MAIN_HAND);
        return true;
    }

    // -- HYBRID mode coordination (used by MixinNetHandlerPlayClient) --

    public boolean isPendingRestore() {
        if (pendingRestoreSession == null) {
            return false;
        }
        if (System.currentTimeMillis() - pendingRestoreTimestamp > RESTORE_TIMEOUT_MS) {
            pendingRestoreSession = null;
            return false;
        }
        return true;
    }

    @Nullable
    public GuiSession getPendingRestoreSession() {
        return pendingRestoreSession;
    }

    public void clearPendingRestore() {
        pendingRestoreSession = null;
    }
}
