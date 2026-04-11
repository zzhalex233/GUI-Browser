package com.zzhalex233.guibrowser.client.runtime;

import com.zzhalex233.guibrowser.config.BrowserConfig;

import java.util.Objects;

public final class GuiBrowserRuntime {
    private static GuiBrowserRuntime instance;

    private final BrowserConfig config;

    private GuiBrowserRuntime(BrowserConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public static GuiBrowserRuntime initialize(BrowserConfig config) {
        instance = new GuiBrowserRuntime(config);
        return instance;
    }

    public static GuiBrowserRuntime getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GuiBrowserRuntime has not been initialized.");
        }
        return instance;
    }

    static void resetForTests() {
        instance = null;
    }

    public BrowserConfig getConfig() {
        return config;
    }
}