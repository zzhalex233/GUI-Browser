package com.zzhalex233.guibrowser.client.chrome;

import com.zzhalex233.guibrowser.client.popup.GuiRestoreFailedToast;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.List;

public final class GuiChromeRenderer {

    private static final int BAR_COLOR = 0xCC222222;
    private static final int TAB_COLOR = 0xCC444444;
    private static final int TAB_ACTIVE_COLOR = 0xCC666666;
    private static final int TAB_STALE_COLOR = 0x66333333;
    private static final int TAB_HOVER_COLOR = 0x44FFFFFF;
    private static final int BUTTON_HOVER_COLOR = 0x44FFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_STALE_COLOR = 0xFF888888;
    private static final int CLOSE_COLOR = 0xFFAAAAAA;
    private static final int CLOSE_HOVER_COLOR = 0xFFFF6666;

    private final GuiChromeOverlayController controller;

    public GuiChromeRenderer(GuiChromeOverlayController controller) {
        this.controller = controller;
    }

    public void render(ScaledResolution resolution, int mouseX, int mouseY,
                       net.minecraft.client.gui.FontRenderer fontRenderer) {
        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();
        GuiChromeLayout layout = new GuiChromeLayout(screenWidth, screenHeight);
        List<GuiSession> tabs = controller.getTabs();
        GuiSession foreground = controller.getForegroundSession();
        GuiChromeTarget hoverTarget = layout.hitTest(mouseX, mouseY, tabs.size());

        boolean depthWasEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
        boolean blendWasEnabled = GL11.glGetBoolean(GL11.GL_BLEND);
        int prevBlendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int prevBlendDst = GL11.glGetInteger(GL11.GL_BLEND_DST);
        int prevBlendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int prevBlendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();

        drawTopBar(layout);
        drawTabs(layout, tabs, foreground, hoverTarget, fontRenderer);
        drawButtons(layout, hoverTarget, fontRenderer);
        drawToast(screenWidth, fontRenderer);

        if (depthWasEnabled) {
            GlStateManager.enableDepth();
        } else {
            GlStateManager.disableDepth();
        }
        if (blendWasEnabled) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }
        GlStateManager.tryBlendFuncSeparate(prevBlendSrc, prevBlendDst, prevBlendSrcAlpha, prevBlendDstAlpha);
        GlStateManager.popMatrix();
    }

    private void drawTopBar(GuiChromeLayout layout) {
        GuiChromeLayout.Rect bar = layout.topBarRect();
        Gui.drawRect(bar.getX(), bar.getY(), bar.getRight(), bar.getBottom(), BAR_COLOR);
    }

    private void drawTabs(GuiChromeLayout layout, List<GuiSession> tabs,
                          GuiSession foreground, GuiChromeTarget hoverTarget,
                          net.minecraft.client.gui.FontRenderer fontRenderer) {
        for (int i = 0; i < tabs.size(); i++) {
            GuiSession tab = tabs.get(i);
            GuiChromeLayout.Rect tabRect = layout.tabRect(i);
            boolean isActive = foreground != null && foreground.getId().equals(tab.getId());
            boolean isHovered = hoverTarget.getType() == GuiChromeTarget.Type.TAB
                    && hoverTarget.getTabIndex() == i;
            boolean isCloseHovered = hoverTarget.getType() == GuiChromeTarget.Type.TAB_CLOSE
                    && hoverTarget.getTabIndex() == i;

            boolean isStale = tab.isStale();
            int bgColor = isStale ? TAB_STALE_COLOR : (isActive ? TAB_ACTIVE_COLOR : TAB_COLOR);
            Gui.drawRect(tabRect.getX(), tabRect.getY(), tabRect.getRight(), tabRect.getBottom(), bgColor);

            if (isHovered && !isCloseHovered) {
                Gui.drawRect(tabRect.getX(), tabRect.getY(), tabRect.getRight(), tabRect.getBottom(), TAB_HOVER_COLOR);
            }

            String rawTitle = isStale ? "~ " + tab.getTitle() : tab.getTitle();
            String title = trimTitle(rawTitle, tabRect.getWidth() - GuiChromeLayout.TAB_CLOSE_SIZE - 6, fontRenderer);
            int textColor = isStale ? TEXT_STALE_COLOR : TEXT_COLOR;
            fontRenderer.drawString(title,
                    tabRect.getX() + 2,
                    tabRect.getY() + (tabRect.getHeight() - 8) / 2,
                    textColor);

            GuiChromeLayout.Rect closeRect = layout.tabCloseRect(i);
            int closeColor = isCloseHovered ? CLOSE_HOVER_COLOR : CLOSE_COLOR;
            fontRenderer.drawString("x",
                    closeRect.getX() + (closeRect.getWidth() - fontRenderer.getStringWidth("x")) / 2,
                    closeRect.getY() + (closeRect.getHeight() - 8) / 2,
                    closeColor);
        }
    }

    private void drawButtons(GuiChromeLayout layout, GuiChromeTarget hoverTarget,
                             net.minecraft.client.gui.FontRenderer fontRenderer) {
        drawButton(layout.bookmarkButtonRect(), "\u2606",
                hoverTarget.getType() == GuiChromeTarget.Type.BOOKMARK_BUTTON, fontRenderer);
        drawButton(layout.historyButtonRect(), "H",
                hoverTarget.getType() == GuiChromeTarget.Type.HISTORY_BUTTON, fontRenderer);
    }

    private void drawButton(GuiChromeLayout.Rect rect, String label, boolean hovered,
                            net.minecraft.client.gui.FontRenderer fontRenderer) {
        if (hovered) {
            Gui.drawRect(rect.getX(), rect.getY(), rect.getRight(), rect.getBottom(), BUTTON_HOVER_COLOR);
        }
        fontRenderer.drawString(label,
                rect.getX() + (rect.getWidth() - fontRenderer.getStringWidth(label)) / 2,
                rect.getY() + (rect.getHeight() - 8) / 2,
                TEXT_COLOR);
    }

    private String trimTitle(String title, int maxWidth, net.minecraft.client.gui.FontRenderer fontRenderer) {
        if (fontRenderer.getStringWidth(title) <= maxWidth) {
            return title;
        }
        String ellipsis = "...";
        int ellipsisWidth = fontRenderer.getStringWidth(ellipsis);
        String trimmed = title;
        while (trimmed.length() > 1 && fontRenderer.getStringWidth(trimmed) + ellipsisWidth > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    private void drawToast(int screenWidth, net.minecraft.client.gui.FontRenderer fontRenderer) {
        if (!GuiRestoreFailedToast.isActive()) return;
        float alpha = GuiRestoreFailedToast.getAlpha();
        String msg = GuiRestoreFailedToast.getMessage();
        int textW = fontRenderer.getStringWidth(msg);
        int toastW = textW + 12;
        int toastX = (screenWidth - toastW) / 2;
        int toastY = GuiChromeLayout.TOP_BAR_HEIGHT + 4;
        int a = (int) (alpha * 0xDD) << 24;
        Gui.drawRect(toastX, toastY, toastX + toastW, toastY + 16, a | 0x331111);
        int textAlpha = (int) (alpha * 255) << 24;
        fontRenderer.drawString(msg, toastX + 6, toastY + 4, textAlpha | 0xFF6666);
    }

    public int getTopBarHeight() {
        return GuiChromeLayout.TOP_BAR_HEIGHT;
    }
}
