package com.zzhalex233.guibrowser.client.session;

import javax.annotation.Nullable;

public class InteractionSourceTracker {

    static final long EXPIRY_TICKS = 40L;

    @Nullable
    private GuiSessionSource pending;
    private long setPendingAtTick;

    public void setPending(GuiSessionSource source, long currentTick) {
        this.pending = source;
        this.setPendingAtTick = currentTick;
    }

    @Nullable
    public GuiSessionSource consumePending(long currentTick) {
        GuiSessionSource source = this.pending;
        if (source == null) {
            return null;
        }
        this.pending = null;
        if (currentTick - this.setPendingAtTick > EXPIRY_TICKS) {
            return null;
        }
        return source;
    }

    public boolean hasPending() {
        return this.pending != null;
    }

    public void clear() {
        this.pending = null;
    }
}
