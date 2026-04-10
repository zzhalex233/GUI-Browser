package com.zzhalex233.guibrowser.config;

import org.lwjgl.input.Keyboard;

import java.util.Objects;

public final class BrowserConfig {
    private final EscAction escAction;
    private final int captureHotkeyKeyCode;

    public BrowserConfig(EscAction escAction, int captureHotkeyKeyCode) {
        this.escAction = Objects.requireNonNull(escAction, "escAction");
        this.captureHotkeyKeyCode = captureHotkeyKeyCode;
    }

    public static BrowserConfig defaults() {
        return new BrowserConfig(EscAction.MINIMIZE, Keyboard.KEY_B);
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

    public BrowserConfig withEscAction(EscAction escAction) {
        return new BrowserConfig(escAction, captureHotkeyKeyCode);
    }
}