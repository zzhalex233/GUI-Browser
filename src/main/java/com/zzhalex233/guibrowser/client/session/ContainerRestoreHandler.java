package com.zzhalex233.guibrowser.client.session;

import com.zzhalex233.guibrowser.client.popup.GuiRestoreFailedToast;
import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.config.ContainerCacheMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class ContainerRestoreHandler {

    private static final long RESTORE_TIMEOUT_MS = 5000L;

    private final ContainerCacheMode cacheMode;
    private final InteractionSourceTracker sourceTracker;
    private final GuiSessionManager sessionManager;

    /** Only used for timeout detection — not for intercepting packets. */
    @Nullable
    private GuiSessionId pendingRestoreSessionId;
    private long pendingRestoreTimestamp;

    public ContainerRestoreHandler(ContainerCacheMode cacheMode,
                                   InteractionSourceTracker sourceTracker,
                                   GuiSessionManager sessionManager) {
        this.cacheMode = cacheMode;
        this.sourceTracker = sourceTracker;
        this.sessionManager = sessionManager;
    }

    /**
     * Attempt to restore a stale tab by re-interacting with its source.
     * Sets sourceTracker pending so that the vanilla SPacketOpenWindow flow
     * will match the new screen back to the existing stale session via
     * registerOrReuseSession.
     */
    public boolean requestRestore(GuiSession session) {
        if (!session.isStale()) {
            return false;
        }
        GuiSessionSource source = session.getSource();
        if (source == null) {
            return false;
        }
        // Reject if another restore is already in flight
        if (pendingRestoreSessionId != null
                && System.currentTimeMillis() - pendingRestoreTimestamp <= RESTORE_TIMEOUT_MS) {
            return false;
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
        if (player.dimension != blockSource.getDimensionId()) {
            return false;
        }

        // Record pending for timeout detection
        pendingRestoreSessionId = session.getId();
        pendingRestoreTimestamp = System.currentTimeMillis();

        // Set source so vanilla's displayGuiScreen → onBeforeDisplay → registerOrReuseSession
        // will match the new screen back to this stale session.
        sourceTracker.setPendingForRestore(blockSource, System.currentTimeMillis());

        // Bypass server-side distance check for integrated server
        GuiBrowserRuntime.getInstance().setBypassServerDistanceCheck(true);

        // Send interaction packet directly (bypasses client-side reach check)
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
            return false;
        }

        pendingRestoreSessionId = session.getId();
        pendingRestoreTimestamp = System.currentTimeMillis();

        sourceTracker.setPendingForRestore(entitySource, System.currentTimeMillis());

        mc.playerController.interactWithEntity(player, entity, EnumHand.MAIN_HAND);
        return true;
    }

    /**
     * Called each render tick. If a restore has been pending longer than the
     * timeout and the session is still stale, show a failure toast.
     */
    public void tickPendingRestore() {
        if (pendingRestoreSessionId == null) {
            return;
        }
        // Check if restore already succeeded (session no longer stale)
        GuiSession session = sessionManager.findSession(pendingRestoreSessionId);
        if (session != null && !session.isStale()) {
            pendingRestoreSessionId = null;
            return;
        }
        // Check timeout
        if (System.currentTimeMillis() - pendingRestoreTimestamp > RESTORE_TIMEOUT_MS) {
            GuiSessionId id = pendingRestoreSessionId;
            pendingRestoreSessionId = null;
            GuiBrowserRuntime.getInstance().setBypassServerDistanceCheck(false);
            if (session != null) {
                GuiRestoreFailedToast.show("Restore timed out: " + session.getTitle());
            }
        }
    }
}
