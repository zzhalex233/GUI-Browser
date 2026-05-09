package com.zzhalex233.guibrowser.client.chrome;

import com.zzhalex233.guibrowser.Reference;
import com.zzhalex233.guibrowser.client.popup.GuiRestoreFailedToast;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource.BlockSource;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayList;
import java.util.List;

public final class GuiChromeRenderer {

    private static final long TAB_TOOLTIP_DELAY_MS = 500L;

    private static final int BUTTON_HOVER_COLOR = 0x44FFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int CLOSE_COLOR = 0xFFAAAAAA;
    private static final int CLOSE_HOVER_COLOR = 0xFFFF6666;
    private static final ResourceLocation CHROME_BAR_TEXTURE =
        new ResourceLocation(Reference.MOD_ID, "textures/gui/chrome_bar.png");
    private static final ResourceLocation TAB_TEXTURE =
        new ResourceLocation(Reference.MOD_ID, "textures/gui/tab.png");
    private static final ResourceLocation TAB_ACTIVE_TEXTURE =
        new ResourceLocation(Reference.MOD_ID, "textures/gui/tab_active.png");
    private static final ResourceLocation TAB_HOVER_TEXTURE =
        new ResourceLocation(Reference.MOD_ID, "textures/gui/tab_hover.png");
    private static final ResourceLocation BOOKMARK_ICON =
        new ResourceLocation(Reference.MOD_ID, "textures/gui/icon_bookmark.png");
    private static final ResourceLocation HISTORY_ICON =
        new ResourceLocation(Reference.MOD_ID, "textures/gui/icon_history.png");
    private static final ResourceLocation TAB_ICON =
        new ResourceLocation(Reference.MOD_ID, "textures/gui/icon_tab.png");
    private static final ResourceLocation CLOSE_ICON =
        new ResourceLocation(Reference.MOD_ID, "textures/gui/icon_close.png");

    private final GuiChromeOverlayController controller;
    private int hoveredTabIndex = -1;
    private long hoveredTabSince = 0L;

    public GuiChromeRenderer(GuiChromeOverlayController controller) {
        this.controller = controller;
    }

    public void render(ScaledResolution resolution, int mouseX, int mouseY,
                       net.minecraft.client.gui.FontRenderer fontRenderer) {
        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();
        GuiChromeLayout layout = controller.createLayout(screenWidth, screenHeight);
        List<GuiSession> tabs = controller.getTabs();
        GuiSession foreground = controller.getForegroundSession();
        GuiChromeTarget hoverTarget = layout.hitTest(mouseX, mouseY, tabs.size());
        updateHoverState(hoverTarget);

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
        GuiRestoreFailedToast.draw(screenWidth, fontRenderer, GuiChromeLayout.TOP_BAR_HEIGHT + 4);
        drawTabTooltip(layout, tabs, hoverTarget, mouseX, mouseY, fontRenderer);

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
        drawTexture(CHROME_BAR_TEXTURE, bar);
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

            drawTexture(isActive ? TAB_ACTIVE_TEXTURE : TAB_TEXTURE, tabRect);

            if (isHovered && !isCloseHovered) {
                drawTexture(TAB_HOVER_TEXTURE, tabRect);
            }

            drawTabIcon(tab, tabRect);
            int titleX = tabRect.getX() + GuiChromeLayout.TAB_LEFT_PADDING + GuiChromeLayout.TAB_ICON_SIZE + 4;
            int titleMaxWidth = tabRect.getRight() - titleX - GuiChromeLayout.TAB_CLOSE_SIZE - 8;
            String title = trimTitle(tab.getTitle(), titleMaxWidth, fontRenderer);
            fontRenderer.drawString(title,
                    titleX,
                    tabRect.getY() + (tabRect.getHeight() - 8) / 2,
                    TEXT_COLOR);

            GuiChromeLayout.Rect closeRect = layout.tabCloseRect(i);
            drawIcon(CLOSE_ICON, closeRect, isCloseHovered ? CLOSE_HOVER_COLOR : CLOSE_COLOR);
        }
    }

    private void drawTabIcon(GuiSession tab, GuiChromeLayout.Rect tabRect) {
        ItemStack icon = resolveBlockIcon(tab);
        int x = tabRect.getX() + GuiChromeLayout.TAB_LEFT_PADDING;
        int y = tabRect.getY() + (tabRect.getHeight() - GuiChromeLayout.TAB_ICON_SIZE) / 2;
        if (icon.isEmpty()) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(TAB_ICON);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0,
                GuiChromeLayout.TAB_ICON_SIZE, GuiChromeLayout.TAB_ICON_SIZE, 16, 16);
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableDepth();
        GuiItemRenderState.withGuiItemLighting(() ->
            mc.getRenderItem().renderItemAndEffectIntoGUI(icon, x - 1, y - 1));
        GlStateManager.disableDepth();
    }

    private ItemStack resolveBlockIcon(GuiSession tab) {
        GuiSessionSource source = tab.getSource();
        if (!(source instanceof GuiSessionSource.BlockSource)) {
            return ItemStack.EMPTY;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return ItemStack.EMPTY;
        }
        GuiSessionSource.BlockSource blockSource = (GuiSessionSource.BlockSource) source;
        if (blockSource.getDimensionId() != (mc.player == null ? Integer.MIN_VALUE : mc.player.dimension)) {
            return ItemStack.EMPTY;
        }
        if (!mc.world.isBlockLoaded(blockSource.getPos())) {
            return ItemStack.EMPTY;
        }
        IBlockState state = mc.world.getBlockState(blockSource.getPos());
        ItemStack stack = state.getBlock().getPickBlock(state, null, mc.world, blockSource.getPos(), mc.player);
        if (!stack.isEmpty()) {
            return stack;
        }
        Item item = Item.getItemFromBlock(state.getBlock());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1, state.getBlock().damageDropped(state));
    }

    private void drawButtons(GuiChromeLayout layout, GuiChromeTarget hoverTarget,
                             net.minecraft.client.gui.FontRenderer fontRenderer) {
        drawButton(layout.bookmarkButtonRect(), BOOKMARK_ICON,
                hoverTarget.getType() == GuiChromeTarget.Type.BOOKMARK_BUTTON, fontRenderer);
        drawButton(layout.historyButtonRect(), HISTORY_ICON,
                hoverTarget.getType() == GuiChromeTarget.Type.HISTORY_BUTTON, fontRenderer);
    }

    private void drawButton(GuiChromeLayout.Rect rect, ResourceLocation icon, boolean hovered,
                            net.minecraft.client.gui.FontRenderer fontRenderer) {
        if (hovered) {
            Gui.drawRect(rect.getX(), rect.getY(), rect.getRight(), rect.getBottom(), BUTTON_HOVER_COLOR);
        }
        drawIcon(icon, rect, TEXT_COLOR);
    }

    private void drawIcon(ResourceLocation icon, GuiChromeLayout.Rect rect, int color) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(icon);
        float a = ((color >> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GlStateManager.color(r, g, b, a);
        Gui.drawModalRectWithCustomSizedTexture(
            rect.getX(), rect.getY(), 0, 0, rect.getWidth(), rect.getHeight(), 16, 16);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawTexture(ResourceLocation texture, GuiChromeLayout.Rect rect) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(
            rect.getX(), rect.getY(), 0, 0, rect.getWidth(), rect.getHeight(), 16, 16);
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

    private void drawTabTooltip(GuiChromeLayout layout, List<GuiSession> tabs, GuiChromeTarget hoverTarget,
                                int mouseX, int mouseY, net.minecraft.client.gui.FontRenderer fontRenderer) {
        if (hoverTarget.getType() != GuiChromeTarget.Type.TAB) {
            return;
        }
        if (!isTooltipReadyForHoveredTab(hoverTarget.getTabIndex())) {
            return;
        }
        int index = hoverTarget.getTabIndex();
        if (index < 0 || index >= tabs.size()) {
            return;
        }
        GuiSession tab = tabs.get(index);
        drawTooltip(buildTabTooltipLines(tab), mouseX, mouseY, fontRenderer);
    }

    private void updateHoverState(GuiChromeTarget hoverTarget) {
        if (hoverTarget.getType() == GuiChromeTarget.Type.TAB) {
            if (hoveredTabIndex != hoverTarget.getTabIndex()) {
                hoveredTabIndex = hoverTarget.getTabIndex();
                hoveredTabSince = System.currentTimeMillis();
            }
            return;
        }
        hoveredTabIndex = -1;
        hoveredTabSince = 0L;
    }

    private boolean isTooltipReadyForHoveredTab(int tabIndex) {
        return hoveredTabIndex == tabIndex
            && hoveredTabSince > 0L
            && System.currentTimeMillis() - hoveredTabSince >= TAB_TOOLTIP_DELAY_MS;
    }

    private List<String> buildTabTooltipLines(GuiSession tab) {
        List<String> lines = new ArrayList<>();
        GuiSessionSource source = tab.getSource();
        if (source instanceof BlockSource) {
            BlockSource blockSource = (BlockSource) source;
            lines.add("x=" + blockSource.getPos().getX()
                + ", y=" + blockSource.getPos().getY()
                + ", z=" + blockSource.getPos().getZ());
        }
        return lines;
    }

    private void drawTooltip(List<String> lines, int mouseX, int mouseY,
                             net.minecraft.client.gui.FontRenderer fontRenderer) {
        if (lines.isEmpty()) {
            return;
        }
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, fontRenderer.getStringWidth(line));
        }
        int x = mouseX + 8;
        int y = mouseY + 8;
        int lineHeight = 10;
        int height = lines.size() * lineHeight + 1;
        Gui.drawRect(x - 3, y - 3, x + width + 3, y + height, 0xEE111111);
        for (int i = 0; i < lines.size(); i++) {
            fontRenderer.drawString(lines.get(i), x, y + i * lineHeight, TEXT_COLOR);
        }
    }

    public int getTopBarHeight() {
        return GuiChromeLayout.TOP_BAR_HEIGHT;
    }
}
