package com.zzhalex233.guibrowser.client.session;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class GuiSessionId {
    private static final AtomicLong NEXT_ID = new AtomicLong(1L);

    private final long value;

    private GuiSessionId(long value) {
        this.value = value;
    }

    public static GuiSessionId create() {
        return new GuiSessionId(NEXT_ID.getAndIncrement());
    }

    public long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GuiSessionId)) {
            return false;
        }
        GuiSessionId that = (GuiSessionId) other;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}