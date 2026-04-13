package com.zzhalex233.guibrowser.client.session;

import javax.annotation.Nullable;

public class InteractionSourceTracker {

    static final long EXPIRY_MS = 2000L;

    @Nullable
    private GuiSessionSource pending;
    private long setPendingAtMs;

    public void setPending(GuiSessionSource source, long currentTimeMs) {
        this.pending = source;
        this.setPendingAtMs = currentTimeMs;
    }

    @Nullable
    public GuiSessionSource consumePending(long currentTimeMs) {
        GuiSessionSource source = this.pending;
        if (source == null) {
            return null;
        }
        this.pending = null;
        if (currentTimeMs - this.setPendingAtMs > EXPIRY_MS) {
            return null;
        }
        return source;
    }

    public boolean hasPending(long currentTimeMs) {
        return this.pending != null && (currentTimeMs - this.setPendingAtMs) <= EXPIRY_MS;
    }

    public void clear() {
        this.pending = null;
    }
}
