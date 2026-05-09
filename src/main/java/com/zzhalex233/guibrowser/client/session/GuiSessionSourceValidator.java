package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.World;
import com.zzhalex233.guibrowser.client.history.GuiHistoryStore;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GuiSessionSourceValidator {
    private final GuiSessionManager sessionManager;

    public GuiSessionSourceValidator(GuiSessionManager sessionManager) {
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
    }

    public boolean pruneInvalidSessions(@Nullable World world, int currentDimensionId, @Nullable GuiScreen currentScreen) {
        return pruneInvalidSessions(world, currentDimensionId, currentScreen, null);
    }

    public boolean pruneInvalidSessions(@Nullable World world, int currentDimensionId, @Nullable GuiScreen currentScreen,
                                        @Nullable GuiHistoryStore historyStore) {
        if (world == null) {
            return false;
        }

        boolean closedForeground = false;
        List<GuiSession> sessions = new ArrayList<>(sessionManager.listAllSessions());
        for (GuiSession session : sessions) {
            if (isInvalid(session.getSource(), world, currentDimensionId)) {
                if (session.getScreen() == currentScreen) {
                    closedForeground = true;
                }
                sessionManager.destroySession(session.getId(), false);
                if (historyStore != null && session.getSource() instanceof GuiSessionSource.BlockSource) {
                    historyStore.remove(GuiSessionSourceKey.fromBlockSource((GuiSessionSource.BlockSource) session.getSource()));
                }
            }
        }
        return closedForeground;
    }

    private boolean isInvalid(@Nullable GuiSessionSource source, World world, int currentDimensionId) {
        if (source == null) {
            return false;
        }

        if (source instanceof GuiSessionSource.BlockSource) {
            GuiSessionSource.BlockSource blockSource = (GuiSessionSource.BlockSource) source;
            if (blockSource.getDimensionId() != currentDimensionId) {
                return false;
            }
            if (!world.isBlockLoaded(blockSource.getPos())) {
                return false;
            }
            return world.isAirBlock(blockSource.getPos());
        }

        return false;
    }
}
