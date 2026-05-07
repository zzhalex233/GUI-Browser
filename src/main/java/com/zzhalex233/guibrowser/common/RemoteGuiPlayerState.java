package com.zzhalex233.guibrowser.common;

public final class RemoteGuiPlayerState {
    private boolean usingRemoteGui;
    private boolean checkRemoteGuiUsage;
    private boolean allowNextCloseForRemoteTransition;

    public void setRemoteGuiInteraction() {
        this.usingRemoteGui = true;
        this.checkRemoteGuiUsage = true;
    }

    public void allowNextCloseForRemoteTransition() {
        this.allowNextCloseForRemoteTransition = true;
    }

    public void onCloseRemoteGui() {
        if (this.allowNextCloseForRemoteTransition) {
            this.allowNextCloseForRemoteTransition = false;
            return;
        }
        if (this.usingRemoteGui) {
            this.usingRemoteGui = false;
            this.checkRemoteGuiUsage = false;
        }
    }

    public boolean canInteractWithRemoteGui() {
        if (this.usingRemoteGui) {
            this.checkRemoteGuiUsage = false;
            return true;
        }
        if (this.checkRemoteGuiUsage) {
            this.checkRemoteGuiUsage = false;
        }
        return false;
    }
}
