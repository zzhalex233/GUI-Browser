package com.zzhalex233.guibrowser.client.runtime;

import com.zzhalex233.guibrowser.config.BrowserConfig;
import com.zzhalex233.guibrowser.config.EscAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GuiBrowserRuntimeTest {

    @AfterEach
    void resetRuntime() {
        GuiBrowserRuntime.resetForTests();
    }

    @Test
    void initializeStoresConfigAndReturnsSingletonInstance() {
        BrowserConfig config = new BrowserConfig(EscAction.CLOSE, 65);

        GuiBrowserRuntime runtime = GuiBrowserRuntime.initialize(config);

        assertSame(runtime, GuiBrowserRuntime.getInstance());
        assertEquals(config, runtime.getConfig());
        assertEquals(65, runtime.getConfig().getCaptureHotkeyKeyCode());
        assertNotNull(runtime.getSessionManager());
        assertNotNull(runtime.getLifecycleBridge());
    }
}
