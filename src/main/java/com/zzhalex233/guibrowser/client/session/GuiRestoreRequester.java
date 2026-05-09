package com.zzhalex233.guibrowser.client.session;

public interface GuiRestoreRequester {
    boolean requestRestore(GuiSession session);

    boolean requestSync(GuiSession session);

    boolean requestRestore(String title, GuiSessionSource source);
}
