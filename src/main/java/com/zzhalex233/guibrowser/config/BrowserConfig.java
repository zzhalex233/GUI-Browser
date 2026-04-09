package com.zzhalex233.guibrowser.config;

import org.lwjgl.input.Keyboard;

import java.util.Objects;

public final class BrowserConfig {
    private final EscAction escAction;
    private final int openBrowserKeyCode;

    public BrowserConfig(EscAction escAction, int openBrowserKeyCode) {
        this.escAction = Objects.requireNonNull(escAction, "escAction");
        this.openBrowserKeyCode = openBrowserKeyCode;
    }

    public static BrowserConfig defaults() {
        return new BrowserConfig(EscAction.MINIMIZE, Keyboard.KEY_B);
    }

    public EscAction getEscAction() {
        return escAction;
    }

    public int getOpenBrowserKeyCode() {
        return openBrowserKeyCode;
    }
}
