package com.zzhalex233.guibrowser.client.gui;

import com.zzhalex233.guibrowser.client.browser.BrowserShellController;
import com.zzhalex233.guibrowser.client.browser.BrowserWindowState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class BrowserRootGui extends GuiScreen {
    private static final int WINDOW_COLOR = 0xF01B1F27;
    private static final int TITLE_BAR_COLOR = 0xF02A3140;
    private static final int TAB_STRIP_COLOR = 0xF0222833;
    private static final int CONTENT_COLOR = 0xEE10151D;
    private static final int BORDER_COLOR = 0xFF6F8BA6;
    private static final int BUTTON_COLOR = 0xFF43536A;
    private static final int BUTTON_HOVER_COLOR = 0xFF5E748F;

    private final BrowserShellController controller;

    public BrowserRootGui(BrowserShellController controller) {
        this.controller = controller;
    }

    @Override
    public void initGui() {
        controller.getManager().getWindowState().clampToViewport(width, height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        BrowserWindowState window = controller.getManager().getWindowState();
        window.clampToViewport(width, height);

        BrowserChromeLayout.Rect titleBar = BrowserChromeLayout.titleBar(window);
        BrowserChromeLayout.Rect tabStrip = BrowserChromeLayout.tabStrip(window);
        BrowserChromeLayout.Rect contentArea = BrowserChromeLayout.contentArea(window);
        BrowserChromeLayout.Rect minimizeButton = BrowserChromeLayout.minimizeButton(window);
        BrowserChromeLayout.Rect closeButton = BrowserChromeLayout.closeButton(window);

        Gui.drawRect(window.getX(), window.getY(), window.getX() + window.getWidth(), window.getY() + window.getHeight(), WINDOW_COLOR);
        Gui.drawRect(window.getX(), window.getY(), window.getX() + window.getWidth(), window.getY() + 1, BORDER_COLOR);
        Gui.drawRect(window.getX(), window.getY() + window.getHeight() - 1, window.getX() + window.getWidth(), window.getY() + window.getHeight(), BORDER_COLOR);
        Gui.drawRect(window.getX(), window.getY(), window.getX() + 1, window.getY() + window.getHeight(), BORDER_COLOR);
        Gui.drawRect(window.getX() + window.getWidth() - 1, window.getY(), window.getX() + window.getWidth(), window.getY() + window.getHeight(), BORDER_COLOR);

        Gui.drawRect(titleBar.getX(), titleBar.getY(), titleBar.getRight(), titleBar.getBottom(), TITLE_BAR_COLOR);
        Gui.drawRect(tabStrip.getX(), tabStrip.getY(), tabStrip.getRight(), tabStrip.getBottom(), TAB_STRIP_COLOR);
        Gui.drawRect(contentArea.getX(), contentArea.getY(), contentArea.getRight(), contentArea.getBottom(), CONTENT_COLOR);

        drawButton(minimizeButton, mouseX, mouseY, "-");
        drawButton(closeButton, mouseX, mouseY, "x");

        fontRenderer.drawStringWithShadow("GUI Browser", titleBar.getX() + 6, titleBar.getY() + 6, 0xFFFFFFFF);
        fontRenderer.drawString("No tabs yet", tabStrip.getX() + 6, tabStrip.getY() + 7, 0xFFD7E2F0);

        int textY = contentArea.getY() + 12;
        fontRenderer.drawString("No tabs", contentArea.getX() + 10, textY, 0xFFFFFFFF);
        fontRenderer.drawString("Press the browser hotkey while in-world", contentArea.getX() + 10, textY + 14, 0xFFB5C3D3);
        fontRenderer.drawString("Phase 1 only shows the empty browser shell.", contentArea.getX() + 10, textY + 28, 0xFF8FA1B5);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawButton(BrowserChromeLayout.Rect rect, int mouseX, int mouseY, String label) {
        boolean hovered = rect.contains(mouseX, mouseY);
        Gui.drawRect(rect.getX(), rect.getY(), rect.getRight(), rect.getBottom(), hovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
        fontRenderer.drawString(label, rect.getX() + 4, rect.getY() + 2, 0xFFFFFFFF);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            controller.handleEscFromRoot();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        BrowserWindowState window = controller.getManager().getWindowState();
        BrowserChromeLayout.Rect minimizeButton = BrowserChromeLayout.minimizeButton(window);
        BrowserChromeLayout.Rect closeButton = BrowserChromeLayout.closeButton(window);
        BrowserChromeLayout.Rect dragRegion = BrowserChromeLayout.dragRegion(window);

        if (mouseButton == 0) {
            if (closeButton.contains(mouseX, mouseY) || minimizeButton.contains(mouseX, mouseY)) {
                controller.handleChromeCloseOrMinimize();
                return;
            }
            if (dragRegion.contains(mouseX, mouseY)) {
                window.beginDrag(mouseX, mouseY, mouseX - window.getX(), mouseY - window.getY());
                return;
            }
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
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        controller.getManager().getWindowState().endDrag();
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void onGuiClosed() {
        controller.getManager().getWindowState().endDrag();
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
