package com.zzhalex233.guibrowser.client.event;

import com.zzhalex233.guibrowser.client.browser.BrowserShellController;
import com.zzhalex233.guibrowser.client.browser.BrowserState;
import com.zzhalex233.guibrowser.client.gui.BrowserRootGui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientForgeEventHandler {
    private final BrowserShellController controller;

    public ClientForgeEventHandler(BrowserShellController controller) {
        this.controller = controller;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        event.setGui(rewriteOpenedGui(event.getGui()));
    }

    private GuiScreen rewriteOpenedGui(GuiScreen incomingGui) {
        if (incomingGui instanceof BrowserRootGui) {
            return incomingGui;
        }

        if (incomingGui == null) {
            if (controller.isDelegatingToHostedContent()) {
                controller.clearHostedContent();
                return new BrowserRootGui(controller);
            }
            controller.handleExternalCloseSignal();
            return null;
        }

        if (!controller.isInWorld()) {
            controller.clearCaptureRequest();
            if (controller.getManager().getState() != BrowserState.CLOSED) {
                controller.closeBrowser();
            }
            return incomingGui;
        }

        if (controller.isDelegatingToHostedContent()) {
            controller.captureHostedContent(incomingGui, deriveTabTitle(incomingGui));
            return new BrowserRootGui(controller);
        }

        if (controller.hasPendingCaptureRequest() && shouldCapture(incomingGui)) {
            controller.consumeCaptureRequest();
            closeHostedGuiIfNeeded();
            controller.captureHostedContent(incomingGui, deriveTabTitle(incomingGui));
            return new BrowserRootGui(controller);
        }

        if (controller.getManager().getState() != BrowserState.CLOSED && controller.isBrowserRootActive()) {
            controller.closeBrowser();
        }
        return incomingGui;
    }

    private void closeHostedGuiIfNeeded() {
        Object hostedContent = controller.getHostedContent();
        if (hostedContent instanceof GuiScreen) {
            ((GuiScreen) hostedContent).onGuiClosed();
        }
    }

    private static boolean shouldCapture(GuiScreen incomingGui) {
        return !(incomingGui instanceof GuiChat)
            && !(incomingGui instanceof GuiIngameMenu)
            && !(incomingGui instanceof GuiGameOver);
    }

    private static String deriveTabTitle(GuiScreen incomingGui) {
        String simpleName = incomingGui.getClass().getSimpleName();
        if (simpleName == null || simpleName.trim().isEmpty()) {
            return "GUI";
        }
        if (simpleName.startsWith("Gui") && simpleName.length() > 3) {
            simpleName = simpleName.substring(3);
        }
        return simpleName.isEmpty() ? "GUI" : simpleName;
    }
}