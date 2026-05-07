package com.zzhalex233.guibrowser.common;

public interface RemoteGuiPlayer {
    void guibrowser$allowNextCloseForRemoteTransition();

    void guibrowser$setRemoteGuiInteraction();

    void guibrowser$onCloseRemoteGui();

    boolean guibrowser$canInteractWithRemoteGui();
}
