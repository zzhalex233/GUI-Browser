package com.zzhalex233.guibrowser.client.browser;

import com.zzhalex233.guibrowser.client.gui.BrowserRootGui;
import net.minecraft.client.Minecraft;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class MinecraftBrowserShellHost implements BrowserShellHost {
    private static final Method RIGHT_CLICK_MOUSE_METHOD = resolveRightClickMouseMethod();

    @Override
    public boolean isInWorld() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.player != null && minecraft.world != null;
    }

    @Override
    public boolean isBrowserRootActive() {
        return Minecraft.getMinecraft().currentScreen instanceof BrowserRootGui;
    }

    @Override
    public boolean canTriggerQuickCapture() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return isInWorld() && minecraft.currentScreen == null;
    }

    @Override
    public void showBrowserRoot(BrowserShellController controller) {
        Minecraft.getMinecraft().displayGuiScreen(new BrowserRootGui(controller));
    }

    @Override
    public void prepareToCloseBrowser(BrowserShellController controller) {
        if (Minecraft.getMinecraft().currentScreen instanceof BrowserRootGui) {
            ((BrowserRootGui) Minecraft.getMinecraft().currentScreen).closeHostedGuiForShutdown();
        }
    }

    @Override
    public void closeCurrentScreen() {
        Minecraft.getMinecraft().displayGuiScreen(null);
    }

    @Override
    public void performQuickCaptureInteraction(BrowserShellController controller) {
        try {
            RIGHT_CLICK_MOUSE_METHOD.invoke(Minecraft.getMinecraft());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to trigger Minecraft rightClickMouse for GUI capture.", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Minecraft rightClickMouse GUI capture bridge failed.", cause);
        }
    }

    private static Method resolveRightClickMouseMethod() {
        try {
            Method method = Minecraft.class.getDeclaredMethod("rightClickMouse");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to resolve Minecraft.rightClickMouse for GUI capture.", e);
        }
    }
}