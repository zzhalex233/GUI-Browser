package com.zzhalex233.guibrowser.client.gui;

import com.zzhalex233.guibrowser.client.browser.BrowserShellController;
import com.zzhalex233.guibrowser.client.browser.BrowserState;
import com.zzhalex233.guibrowser.client.browser.BrowserWindowState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BrowserRootGui extends GuiScreen {
    private static final int HOVER_OVERLAY_COLOR = 0x66505050;
    private static final int TAB_TEXT_COLOR = 0xFF222222;
    private static final int EMPTY_TITLE_COLOR = 0xFF222222;
    private static final int EMPTY_BODY_COLOR = 0xFF4A4A4A;
    private static final Method KEY_TYPED_METHOD = resolveGuiMethod("keyTyped", char.class, int.class);
    private static final Method MOUSE_CLICKED_METHOD = resolveGuiMethod("mouseClicked", int.class, int.class, int.class);
    private static final Method MOUSE_RELEASED_METHOD = resolveGuiMethod("mouseReleased", int.class, int.class, int.class);
    private static final Method MOUSE_CLICK_MOVE_METHOD = resolveGuiMethod("mouseClickMove", int.class, int.class, int.class, long.class);

    private final BrowserShellController controller;
    private Object initializedHostedContent;
    private int initializedContentWidth = -1;
    private int initializedContentHeight = -1;

    public BrowserRootGui(BrowserShellController controller) {
        this.controller = controller;
    }

    @Override
    public void initGui() {
        controller.getManager().getWindowState().clampToViewport(width, height);
        syncHostedGuiToContentArea();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        BrowserWindowState window = controller.getManager().getWindowState();
        window.clampToViewport(width, height);

        BrowserChromeLayout.Rect frame = BrowserChromeLayout.frame(window);
        BrowserChromeLayout.Rect contentArea = BrowserChromeLayout.contentArea(window);
        BrowserChromeLayout.Rect activeTab = hasHostedGui() ? BrowserChromeLayout.tab(window, 0) : null;
        BrowserChromeLayout.Rect minimizeButton = BrowserChromeLayout.minimizeButton(window);
        BrowserChromeLayout.Rect closeButton = BrowserChromeLayout.closeButton(window);

        mc.getTextureManager().bindTexture(BrowserChromeTexture.ATLAS);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawAtlasRect(frame.getX(), frame.getY(), BrowserChromeTexture.FRAME_U, BrowserChromeTexture.FRAME_V,
            BrowserChromeTexture.FRAME_WIDTH, BrowserChromeTexture.FRAME_HEIGHT);

        if (activeTab != null) {
            drawAtlasRect(activeTab.getX(), activeTab.getY(), BrowserChromeTexture.TAB_U, BrowserChromeTexture.TAB_V,
                BrowserChromeTexture.TAB_WIDTH, BrowserChromeTexture.TAB_HEIGHT);
            drawActiveTabTitle(activeTab);
            if (activeTab.contains(mouseX, mouseY)) {
                drawHoverOverlay(activeTab);
            }
        }

        if (minimizeButton.contains(mouseX, mouseY)) {
            drawHoverOverlay(minimizeButton);
        }
        if (closeButton.contains(mouseX, mouseY)) {
            drawHoverOverlay(closeButton);
        }

        if (hasHostedGui()) {
            syncHostedGuiToContentArea(contentArea);
            drawHostedGui(contentArea, mouseX, mouseY, partialTicks);
        } else {
            drawEmptyContent(contentArea);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen() {
        syncHostedGuiToContentArea();
        GuiScreen hostedGui = getHostedGui();
        if (hostedGui != null) {
            withHostedGuiContext(hostedGui, true, new GuiAction() {
                @Override
                public void run(GuiScreen gui) {
                    gui.updateScreen();
                }
            });
        }
        super.updateScreen();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        GuiScreen hostedGui = getHostedGui();
        if (hostedGui != null) {
            BrowserState stateBefore = controller.getManager().getState();
            Object contentBefore = controller.getHostedContent();
            withHostedGuiContext(hostedGui, true, new GuiIoAction() {
                @Override
                public void run(GuiScreen gui) throws IOException {
                    invokeKeyTyped(gui, typedChar, keyCode);
                }
            });
            if (stateBefore != controller.getManager().getState() || contentBefore != controller.getHostedContent()) {
                return;
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                controller.handleEscFromRoot();
            }
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            controller.handleEscFromRoot();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        BrowserWindowState window = controller.getManager().getWindowState();
        BrowserChromeLayout.Rect activeTab = hasHostedGui() ? BrowserChromeLayout.tab(window, 0) : null;
        BrowserChromeLayout.Rect minimizeButton = BrowserChromeLayout.minimizeButton(window);
        BrowserChromeLayout.Rect closeButton = BrowserChromeLayout.closeButton(window);
        BrowserChromeLayout.Rect dragRegion = BrowserChromeLayout.dragRegion(window);
        BrowserChromeLayout.Rect contentArea = BrowserChromeLayout.contentArea(window);

        if (mouseButton == 0) {
            if (closeButton.contains(mouseX, mouseY) || minimizeButton.contains(mouseX, mouseY)) {
                controller.handleChromeCloseOrMinimize();
                return;
            }
            if (activeTab != null && activeTab.contains(mouseX, mouseY)) {
                return;
            }
            if (dragRegion.contains(mouseX, mouseY)) {
                window.beginDrag(mouseX, mouseY, mouseX - window.getX(), mouseY - window.getY());
                return;
            }
        }

        GuiScreen hostedGui = getHostedGui();
        if (hostedGui != null && contentArea.contains(mouseX, mouseY)) {
            syncHostedGuiToContentArea(contentArea);
            final int localMouseX = mouseX - contentArea.getX();
            final int localMouseY = mouseY - contentArea.getY();
            withHostedGuiContext(hostedGui, true, new GuiIoAction() {
                @Override
                public void run(GuiScreen gui) throws IOException {
                    invokeMouseClicked(gui, localMouseX, localMouseY, mouseButton);
                }
            });
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        BrowserWindowState window = controller.getManager().getWindowState();
        if (clickedMouseButton == 0 && window.isDragging()) {
            window.dragTo(mouseX, mouseY, width, height);
            return;
        }

        GuiScreen hostedGui = getHostedGui();
        if (hostedGui != null) {
            BrowserChromeLayout.Rect contentArea = BrowserChromeLayout.contentArea(window);
            final int localMouseX = mouseX - contentArea.getX();
            final int localMouseY = mouseY - contentArea.getY();
            withHostedGuiContext(hostedGui, true, new GuiAction() {
                @Override
                public void run(GuiScreen gui) {
                    invokeMouseClickMove(gui, localMouseX, localMouseY, clickedMouseButton, timeSinceLastClick);
                }
            });
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        controller.getManager().getWindowState().endDrag();

        GuiScreen hostedGui = getHostedGui();
        if (hostedGui != null) {
            BrowserChromeLayout.Rect contentArea = BrowserChromeLayout.contentArea(controller.getManager().getWindowState());
            final int localMouseX = mouseX - contentArea.getX();
            final int localMouseY = mouseY - contentArea.getY();
            withHostedGuiContext(hostedGui, true, new GuiAction() {
                @Override
                public void run(GuiScreen gui) {
                    invokeMouseReleased(gui, localMouseX, localMouseY, state);
                }
            });
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void onGuiClosed() {
        controller.getManager().getWindowState().endDrag();
        initializedHostedContent = null;
        initializedContentWidth = -1;
        initializedContentHeight = -1;
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public void closeHostedGuiForShutdown() {
        GuiScreen hostedGui = getHostedGui();
        if (hostedGui != null) {
            hostedGui.onGuiClosed();
        }
    }

    private boolean hasHostedGui() {
        return controller.getHostedContent() instanceof GuiScreen;
    }

    private GuiScreen getHostedGui() {
        Object hostedContent = controller.getHostedContent();
        return hostedContent instanceof GuiScreen ? (GuiScreen) hostedContent : null;
    }

    private void drawAtlasRect(int x, int y, int u, int v, int width, int height) {
        drawModalRectWithCustomSizedTexture(x, y, u, v, width, height,
            BrowserChromeTexture.ATLAS_WIDTH, BrowserChromeTexture.ATLAS_HEIGHT);
    }

    private void drawHoverOverlay(BrowserChromeLayout.Rect rect) {
        Gui.drawRect(rect.getX(), rect.getY(), rect.getRight(), rect.getBottom(), HOVER_OVERLAY_COLOR);
    }

    private void drawActiveTabTitle(BrowserChromeLayout.Rect activeTab) {
        String title = fontRenderer.trimStringToWidth(controller.getActiveTabTitle(), activeTab.getWidth() - 6);
        fontRenderer.drawString(title, activeTab.getX() + 3, activeTab.getY() + 8, TAB_TEXT_COLOR);
    }

    private void drawEmptyContent(BrowserChromeLayout.Rect contentArea) {
        int textX = contentArea.getX() + 8;
        int textY = contentArea.getY() + 10;
        fontRenderer.drawString("No tabs", textX, textY, EMPTY_TITLE_COLOR);
        fontRenderer.drawString("Use /guibrowser open to keep the browser shell visible.", textX, textY + 14, EMPTY_BODY_COLOR);
        fontRenderer.drawString("Use the capture hotkey on a GUI-capable target to host it here.", textX, textY + 28, EMPTY_BODY_COLOR);
    }

    private void drawHostedGui(BrowserChromeLayout.Rect contentArea, int mouseX, int mouseY, float partialTicks) {
        GuiScreen hostedGui = getHostedGui();
        if (hostedGui == null) {
            return;
        }

        final int localMouseX = mouseX - contentArea.getX();
        final int localMouseY = mouseY - contentArea.getY();
        beginScissor(contentArea);
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) contentArea.getX(), (float) contentArea.getY(), 0.0F);
        try {
            withHostedGuiContext(hostedGui, false, new GuiAction() {
                @Override
                public void run(GuiScreen gui) {
                    gui.drawScreen(localMouseX, localMouseY, partialTicks);
                }
            });
        } finally {
            GlStateManager.popMatrix();
            endScissor();
        }
    }

    private void syncHostedGuiToContentArea() {
        syncHostedGuiToContentArea(BrowserChromeLayout.contentArea(controller.getManager().getWindowState()));
    }

    private void syncHostedGuiToContentArea(BrowserChromeLayout.Rect contentArea) {
        GuiScreen hostedGui = getHostedGui();
        if (hostedGui == null) {
            initializedHostedContent = null;
            initializedContentWidth = -1;
            initializedContentHeight = -1;
            return;
        }
        if (initializedHostedContent == hostedGui
            && initializedContentWidth == contentArea.getWidth()
            && initializedContentHeight == contentArea.getHeight()) {
            return;
        }
        withHostedGuiContext(hostedGui, false, new GuiAction() {
            @Override
            public void run(GuiScreen gui) {
                gui.setWorldAndResolution(mc, contentArea.getWidth(), contentArea.getHeight());
            }
        });
        initializedHostedContent = hostedGui;
        initializedContentWidth = contentArea.getWidth();
        initializedContentHeight = contentArea.getHeight();
    }

    private void withHostedGuiContext(GuiScreen hostedGui, boolean delegateInput, GuiAction action) {
        GuiScreen previousScreen = mc.currentScreen;
        if (delegateInput) {
            controller.beginHostedContentDelegation();
        }
        mc.currentScreen = hostedGui;
        try {
            action.run(hostedGui);
        } finally {
            mc.currentScreen = previousScreen;
            if (delegateInput) {
                controller.endHostedContentDelegation();
            }
        }
    }

    private void withHostedGuiContext(GuiScreen hostedGui, boolean delegateInput, GuiIoAction action) throws IOException {
        GuiScreen previousScreen = mc.currentScreen;
        if (delegateInput) {
            controller.beginHostedContentDelegation();
        }
        mc.currentScreen = hostedGui;
        try {
            action.run(hostedGui);
        } finally {
            mc.currentScreen = previousScreen;
            if (delegateInput) {
                controller.endHostedContentDelegation();
            }
        }
    }

    private void beginScissor(BrowserChromeLayout.Rect rect) {
        ScaledResolution resolution = new ScaledResolution(mc);
        int scaleFactor = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            rect.getX() * scaleFactor,
            (resolution.getScaledHeight() - rect.getBottom()) * scaleFactor,
            rect.getWidth() * scaleFactor,
            rect.getHeight() * scaleFactor
        );
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static void invokeKeyTyped(GuiScreen gui, char typedChar, int keyCode) throws IOException {
        invokeReflectiveIo(KEY_TYPED_METHOD, gui, typedChar, keyCode);
    }

    private static void invokeMouseClicked(GuiScreen gui, int mouseX, int mouseY, int mouseButton) throws IOException {
        invokeReflectiveIo(MOUSE_CLICKED_METHOD, gui, mouseX, mouseY, mouseButton);
    }

    private static void invokeMouseReleased(GuiScreen gui, int mouseX, int mouseY, int state) {
        invokeReflective(MOUSE_RELEASED_METHOD, gui, mouseX, mouseY, state);
    }

    private static void invokeMouseClickMove(GuiScreen gui, int mouseX, int mouseY, int button, long timeSinceLastClick) {
        invokeReflective(MOUSE_CLICK_MOVE_METHOD, gui, mouseX, mouseY, button, timeSinceLastClick);
    }

    private static void invokeReflectiveIo(Method method, GuiScreen gui, Object... arguments) throws IOException {
        try {
            method.invoke(gui, arguments);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to delegate browser child GUI call.", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Delegated browser child GUI call failed.", cause);
        }
    }

    private static void invokeReflective(Method method, GuiScreen gui, Object... arguments) {
        try {
            method.invoke(gui, arguments);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to delegate browser child GUI call.", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Delegated browser child GUI call failed.", cause);
        }
    }

    private static Method resolveGuiMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = GuiScreen.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to resolve GuiScreen method '" + name + "'.", e);
        }
    }

    private interface GuiAction {
        void run(GuiScreen gui);
    }

    private interface GuiIoAction {
        void run(GuiScreen gui) throws IOException;
    }
}