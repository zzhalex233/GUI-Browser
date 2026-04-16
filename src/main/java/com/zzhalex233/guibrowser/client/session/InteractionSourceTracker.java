package com.zzhalex233.guibrowser.client.session;

import javax.annotation.Nullable;

public class InteractionSourceTracker {

    static final long EXPIRY_MS = 2000L;

    @Nullable
    private GuiSessionSource pending;
    private long setPendingAtMs;
    /** When true, only another restore call can overwrite the pending source. */
    private boolean restoreLock;

    public void setPending(GuiSessionSource source, long currentTimeMs) {
        if (restoreLock && !isExpired(currentTimeMs)) {
            return; // Locked by a restore — ignore normal interactions
        }
        this.pending = source;
        this.setPendingAtMs = currentTimeMs;
        this.restoreLock = false;
    }

    /**
     * Set pending with restore priority. Normal interactions cannot overwrite
     * this until it expires or is consumed. Another restore CAN overwrite it.
     */
    public void setPendingForRestore(GuiSessionSource source, long currentTimeMs) {
        this.pending = source;
        this.setPendingAtMs = currentTimeMs;
        this.restoreLock = true;
    }

    @Nullable
    public GuiSessionSource consumePending(long currentTimeMs) {
        GuiSessionSource source = this.pending;
        if (source == null) {
            return null;
        }
        this.pending = null;
        this.restoreLock = false;
        if (isExpired(currentTimeMs)) {
            return null;
        }
        return source;
    }

    public boolean hasPending(long currentTimeMs) {
        return this.pending != null && !isExpired(currentTimeMs);
    }

    public void clear() {
        this.pending = null;
        this.restoreLock = false;
    }

    private boolean isExpired(long currentTimeMs) {
        return currentTimeMs - this.setPendingAtMs > EXPIRY_MS;
    }
}
