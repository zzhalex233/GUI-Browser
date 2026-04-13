package com.zzhalex233.guibrowser.config;

import org.lwjgl.input.Keyboard;

import java.util.Objects;

public final class BrowserConfig {
    private final EscAction escAction;
    private final int captureHotkeyKeyCode;
    private final int maxCachedSessions;
    private final boolean enableBookmarks;
    private final boolean enableHistoryPanel;
    private final ContainerCacheMode containerCacheMode;

    public BrowserConfig(EscAction escAction, int captureHotkeyKeyCode) {
        this(escAction, captureHotkeyKeyCode, 16, true, true, ContainerCacheMode.HYBRID);
    }

    public BrowserConfig(EscAction escAction, int captureHotkeyKeyCode,
                         int maxCachedSessions, boolean enableBookmarks, boolean enableHistoryPanel) {
        this(escAction, captureHotkeyKeyCode, maxCachedSessions, enableBookmarks, enableHistoryPanel, ContainerCacheMode.HYBRID);
    }

    public BrowserConfig(EscAction escAction, int captureHotkeyKeyCode,
                         int maxCachedSessions, boolean enableBookmarks, boolean enableHistoryPanel,
                         ContainerCacheMode containerCacheMode) {
        this.escAction = Objects.requireNonNull(escAction, "escAction");
        this.captureHotkeyKeyCode = captureHotkeyKeyCode;
        this.maxCachedSessions = maxCachedSessions;
        this.enableBookmarks = enableBookmarks;
        this.enableHistoryPanel = enableHistoryPanel;
        this.containerCacheMode = Objects.requireNonNull(containerCacheMode, "containerCacheMode");
    }

    public static BrowserConfig defaults() {
        return new BrowserConfig(EscAction.MINIMIZE, Keyboard.KEY_B, 16, true, true, ContainerCacheMode.HYBRID);
    }

    public EscAction getEscAction() {
        return escAction;
    }

    public int getCaptureHotkeyKeyCode() {
        return captureHotkeyKeyCode;
    }

    public int getOpenBrowserKeyCode() {
        return captureHotkeyKeyCode;
    }

    public int getMaxCachedSessions() {
        return maxCachedSessions;
    }

    public boolean isEnableBookmarks() {
        return enableBookmarks;
    }

    public boolean isEnableHistoryPanel() {
        return enableHistoryPanel;
    }

    public ContainerCacheMode getContainerCacheMode() {
        return containerCacheMode;
    }

    public BrowserConfig withEscAction(EscAction escAction) {
        return new BrowserConfig(escAction, captureHotkeyKeyCode, maxCachedSessions, enableBookmarks, enableHistoryPanel, containerCacheMode);
    }

    public BrowserConfig withContainerCacheMode(ContainerCacheMode containerCacheMode) {
        return new BrowserConfig(escAction, captureHotkeyKeyCode, maxCachedSessions, enableBookmarks, enableHistoryPanel, containerCacheMode);
    }
}