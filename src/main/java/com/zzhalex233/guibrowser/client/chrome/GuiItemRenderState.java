package com.zzhalex233.guibrowser.client.chrome;

import net.minecraft.client.renderer.RenderHelper;

final class GuiItemRenderState {
    private GuiItemRenderState() {
    }

    static void withGuiItemLighting(Runnable render) {
        RenderHelper.enableGUIStandardItemLighting();
        try {
            render.run();
        } finally {
            RenderHelper.disableStandardItemLighting();
        }
    }
}
