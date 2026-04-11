package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;

import java.util.Objects;

public final class GuiSessionManager {
    private GuiSession foregroundSession;

    public GuiSession registerOpenedSession(GuiScreen screen, String title) {
        Objects.requireNonNull(screen, "screen");
        long now = System.currentTimeMillis();
        if (foregroundSession != null) {
            foregroundSession.clearForeground();
        }
        GuiSession session = new GuiSession(GuiSessionId.create(), screen, GuiSessionTitleResolver.resolve(screen, title), now);
        session.markForeground(now);
        foregroundSession = session;
        return session;
    }

    public GuiSession getForegroundSession() {
        return foregroundSession;
    }
}
