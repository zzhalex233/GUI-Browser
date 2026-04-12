package com.zzhalex233.guibrowser.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrowserConfigLoaderTest {

    @Test
    void saveAndLoadRoundTripMinimalConfig(@TempDir Path tempDir) {
        Path configFile = tempDir.resolve("guibrowser.cfg");

        BrowserConfig original = new BrowserConfig(EscAction.CLOSE, 65);
        BrowserConfigLoader.save(configFile.toFile(), original);

        BrowserConfig loaded = BrowserConfigLoader.load(configFile.toFile());

        assertEquals(EscAction.CLOSE, loaded.getEscAction());
        assertEquals(65, loaded.getCaptureHotkeyKeyCode());
    }

    @Test
    void invalidEscActionFallsBackToDefault(@TempDir Path tempDir) throws Exception {
        Path configFile = tempDir.resolve("guibrowser.cfg");
        java.nio.file.Files.writeString(configFile, "browser.escAction=NOT_A_REAL_ACTION\nbrowser.captureHotkeyKeyCode=66\n");

        BrowserConfig loaded = BrowserConfigLoader.load(configFile.toFile());

        assertEquals(BrowserConfig.defaults().getEscAction(), loaded.getEscAction());
        assertEquals(66, loaded.getCaptureHotkeyKeyCode());
    }

    @Test
    void invalidKeyCodeFallsBackToDefault(@TempDir Path tempDir) throws Exception {
        Path configFile = tempDir.resolve("guibrowser.cfg");
        java.nio.file.Files.writeString(configFile, "browser.escAction=MINIMIZE\nbrowser.captureHotkeyKeyCode=0\n");

        BrowserConfig loaded = BrowserConfigLoader.load(configFile.toFile());

        assertEquals(BrowserConfig.defaults().getCaptureHotkeyKeyCode(), loaded.getCaptureHotkeyKeyCode());
    }

    @Test
    void legacyOpenBrowserKeyFallsBackToCaptureHotkey(@TempDir Path tempDir) throws Exception {
        Path configFile = tempDir.resolve("guibrowser.cfg");
        java.nio.file.Files.writeString(configFile, "browser.escAction=MINIMIZE\nbrowser.openBrowserKeyCode=71\n");

        BrowserConfig loaded = BrowserConfigLoader.load(configFile.toFile());

        assertEquals(71, loaded.getCaptureHotkeyKeyCode());
    }
}
