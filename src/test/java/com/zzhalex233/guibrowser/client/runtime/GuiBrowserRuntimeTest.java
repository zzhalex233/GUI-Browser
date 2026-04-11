package com.zzhalex233.guibrowser.client.runtime;

import com.zzhalex233.guibrowser.config.BrowserConfig;
import com.zzhalex233.guibrowser.config.EscAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    }

    @Test
    void getInstanceWithoutInitializeThrows() {
        IllegalStateException error = assertThrows(IllegalStateException.class, GuiBrowserRuntime::getInstance);

        assertEquals("GuiBrowserRuntime has not been initialized.", error.getMessage());
    }
}