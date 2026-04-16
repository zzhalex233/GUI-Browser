package com.zzhalex233.guibrowser.client.session;

import net.minecraft.client.gui.GuiScreen;

/**
 * Minimal placeholder screen used for stale tabs restored from persistence.
 * Replaced by the real screen when the user clicks the tab and
 * ContainerRestoreHandler triggers a fresh interaction.
 */
public final class StaleTabPlaceholderScreen extends GuiScreen {

    private final String title;

    public StaleTabPlaceholderScreen(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
