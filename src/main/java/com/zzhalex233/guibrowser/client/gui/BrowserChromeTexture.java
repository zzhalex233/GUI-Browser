package com.zzhalex233.guibrowser.client.gui;

import com.zzhalex233.guibrowser.Reference;
import net.minecraft.util.ResourceLocation;

public final class BrowserChromeTexture {
    public static final ResourceLocation ATLAS = new ResourceLocation(Reference.MOD_ID, "textures/gui/browser.png");
    public static final int ATLAS_WIDTH = 256;
    public static final int ATLAS_HEIGHT = 256;

    public static final int FRAME_U = 0;
    public static final int FRAME_V = 0;
    public static final int FRAME_WIDTH = 176;
    public static final int FRAME_HEIGHT = 190;

    public static final int PAGE_X = 0;
    public static final int PAGE_Y = 24;
    public static final int PAGE_WIDTH = 176;
    public static final int PAGE_HEIGHT = 166;

    public static final int TAB_U = 0;
    public static final int TAB_V = 190;
    public static final int TAB_WIDTH = 28;
    public static final int TAB_HEIGHT = 23;
    public static final int TAB_STEP = 30;
    public static final int TAB_BASELINE_Y = 27;

    public static final int MINIMIZE_U = 148;
    public static final int CLOSE_U = 160;
    public static final int BUTTON_V = 13;
    public static final int MINIMIZE_WIDTH = 12;
    public static final int CLOSE_WIDTH = 13;
    public static final int BUTTON_HEIGHT = 14;

    private BrowserChromeTexture() {
    }
}