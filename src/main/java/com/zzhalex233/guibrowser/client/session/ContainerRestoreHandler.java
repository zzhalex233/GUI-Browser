package com.zzhalex233.guibrowser.client.session;

import com.zzhalex233.guibrowser.client.popup.GuiRestoreFailedToast;
import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.config.ContainerCacheMode;
import com.zzhalex233.guibrowser.network.GuiBrowserNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class ContainerRestoreHandler implements GuiRestoreRequester {

    private static final long RESTORE_TIMEOUT_MS = 5000L;

    private final ContainerCacheMode cacheMode;
    private final InteractionSourceTracker sourceTracker;
    private final GuiSessionManager sessionManager;

    @Nullable
    private GuiSessionId pendingRestoreSessionId;
    private long pendingRestoreTimestamp;
    private boolean pendingRestoreShowsTimeoutToast;

    public ContainerRestoreHandler(ContainerCacheMode cacheMode,
                                   InteractionSourceTracker sourceTracker,
                                   GuiSessionManager sessionManager) {
        this.cacheMode = cacheMode;
        this.sourceTracker = sourceTracker;
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean requestRestore(GuiSession session) {
        return requestInteraction(session, true);
    }

    @Override
    public boolean requestSync(GuiSession session) {
        return requestInteraction(session, false);
    }

    private boolean requestInteraction(GuiSession session, boolean showTimeoutToast) {
        GuiSessionSource source = session.getSource();
        if (source == null) {
            return false;
        }
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
            return restoreBlock((GuiSessionSource.BlockSource) source, session, mc, player, showTimeoutToast);
        }
        if (source instanceof GuiSessionSource.EntitySource) {
            return restoreEntity((GuiSessionSource.EntitySource) source, session, mc, player, showTimeoutToast);
        }
        return false;
    }

    private boolean restoreBlock(GuiSessionSource.BlockSource blockSource,
                                 GuiSession session,
                                 Minecraft mc,
                                 EntityPlayerSP player,
                                 boolean showTimeoutToast) {
        BlockPos pos = blockSource.getPos();
        WorldClient world = mc.world;
        if (world == null) {
            return false;
        }
        if (player.dimension != blockSource.getDimensionId()) {
            return false;
        }

        pendingRestoreSessionId = session.getId();
        pendingRestoreTimestamp = System.currentTimeMillis();
        pendingRestoreShowsTimeoutToast = showTimeoutToast;

        GuiBrowserRuntime.getInstance().armMouseWarpSuppression();
        sourceTracker.setPendingForRestore(blockSource, System.currentTimeMillis());
        GuiBrowserNetwork.sendOpenRemoteBlock(
            blockSource.getDimensionId(),
            pos,
            blockSource.getFacing(),
            EnumHand.MAIN_HAND,
            blockSource.getHitX(),
            blockSource.getHitY(),
            blockSource.getHitZ()
        );
        player.swingArm(EnumHand.MAIN_HAND);
        return true;
    }

    private boolean restoreEntity(GuiSessionSource.EntitySource entitySource,
                                  GuiSession session,
                                  Minecraft mc,
                                  EntityPlayerSP player,
                                  boolean showTimeoutToast) {
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
        pendingRestoreShowsTimeoutToast = showTimeoutToast;

        GuiBrowserRuntime.getInstance().armMouseWarpSuppression();
        sourceTracker.setPendingForRestore(entitySource, System.currentTimeMillis());
        mc.playerController.interactWithEntity(player, entity, EnumHand.MAIN_HAND);
        return true;
    }

    public void tickPendingRestore() {
        if (pendingRestoreSessionId == null) {
            return;
        }

        GuiSession session = sessionManager.findSession(pendingRestoreSessionId);
        if (pendingRestoreSessionId.equals(sessionManager.getLastServerWindowSessionId())) {
            clearPendingRestore(false, session);
            return;
        }
        if (session == null) {
            clearPendingRestore(false, null);
            return;
        }
        if (System.currentTimeMillis() - pendingRestoreTimestamp > RESTORE_TIMEOUT_MS) {
            clearPendingRestore(pendingRestoreShowsTimeoutToast, session);
        }
    }

    private void clearPendingRestore(boolean showTimeoutToast, @Nullable GuiSession session) {
        pendingRestoreSessionId = null;
        pendingRestoreShowsTimeoutToast = false;
        GuiBrowserRuntime.getInstance().clearRestoreTransientState();
        if (showTimeoutToast && session != null) {
            GuiRestoreFailedToast.show("Restore timed out: " + session.getTitle());
        }
    }
}
