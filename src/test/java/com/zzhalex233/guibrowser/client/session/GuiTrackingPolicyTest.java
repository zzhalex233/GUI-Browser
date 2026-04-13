package com.zzhalex233.guibrowser.client.session;

import c4.curios.client.gui.FakeCuriosScreen;
import com.zzhalex233.guibrowser.client.popup.FakePopupScreen;
import com.zzhalex233.guibrowser.client.session.GuiTrackingPolicy.TrackingDecision;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMemoryErrorScreen;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.GuiConnecting;
import org.junit.jupiter.api.Test;

import static com.zzhalex233.guibrowser.client.session.GuiTrackingPolicy.TrackingDecision.EXCLUDE;
import static com.zzhalex233.guibrowser.client.session.GuiTrackingPolicy.TrackingDecision.TRACK_AS_TAB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiTrackingPolicyTest {

    @Test
    void nullIsExcluded() {
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(null, true));
    }

    @Test
    void hardExcludedClassesReturnExclude() {
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiMainMenu(), true));
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiMultiplayer(), true));
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiWorldSelection(), true));
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiDisconnected(), true));
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiConnecting(), true));
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiDownloadTerrain(), true));
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiGameOver(), true));
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiScreenWorking(), true));
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiErrorScreen(), true));
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiMemoryErrorScreen(), true));
    }

    @Test
    void guiInventoryIsExcluded() {
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiInventory(), true));
    }

    @Test
    void trinketModScreenIsExcluded() {
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new FakeCuriosScreen(), true));
    }

    @Test
    void modPopupIsExcluded() {
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new FakePopupScreen(), true));
    }

    @Test
    void nonContainerScreenIsExcluded() {
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiChat(), true));
    }

    @Test
    void containerWithSourceIsTracked() {
        assertEquals(TRACK_AS_TAB, GuiTrackingPolicy.decide(new GuiContainer() {}, true));
    }

    @Test
    void containerWithoutSourceIsExcluded() {
        assertEquals(EXCLUDE, GuiTrackingPolicy.decide(new GuiContainer() {}, false));
    }

    @Test
    void shouldTrackBackwardCompatReturnsTrue() {
        assertTrue(GuiTrackingPolicy.shouldTrack(new GuiContainer() {}));
    }
}
