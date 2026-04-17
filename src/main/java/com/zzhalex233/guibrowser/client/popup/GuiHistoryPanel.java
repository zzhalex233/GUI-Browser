package com.zzhalex233.guibrowser.client.popup;

import com.zzhalex233.guibrowser.client.chrome.GuiChromeLayout;
import com.zzhalex233.guibrowser.client.history.GuiHistoryEntry;
import com.zzhalex233.guibrowser.client.history.GuiHistoryStore;
import com.zzhalex233.guibrowser.client.session.ContainerRestoreHandler;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;
import com.zzhalex233.guibrowser.client.session.StaleTabPlaceholderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import javax.annotation.Nullable;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GuiHistoryPanel extends GuiScreen {

    private static final int PANEL_WIDTH = 220;
    private static final int ENTRY_HEIGHT = 16;
    private static final int MAX_VISIBLE = 12;
    private static final int PANEL_BG = 0xEE1A1A1A;
    private static final int ENTRY_HOVER = 0x44FFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF888888;
    private static final int ACTION_COLOR = 0xFF66AAFF;
    private static final int CLEAR_BG = 0x44FF4444;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    private final GuiScreen parentScreen;
    private final GuiHistoryStore historyStore;
    private final GuiSessionManager sessionManager;
    @Nullable
    private final ContainerRestoreHandler restoreHandler;
    @Nullable
    private final File dataDir;
    private int panelX;
    private int panelY;
    private int scrollOffset;
    private List<GuiHistoryEntry> entries = new ArrayList<>();

    public GuiHistoryPanel(GuiScreen parentScreen, GuiHistoryStore historyStore,
                           GuiSessionManager sessionManager,
                           @Nullable ContainerRestoreHandler restoreHandler,
                           @Nullable File dataDir) {
        this.parentScreen = parentScreen;
        this.historyStore = historyStore;
        this.sessionManager = sessionManager;
        this.restoreHandler = restoreHandler;
        this.dataDir = dataDir;
    }

    @Override
    public void initGui() {
        super.initGui();
        refreshEntries();
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        GuiChromeLayout layout = new GuiChromeLayout(sr.getScaledWidth(), sr.getScaledHeight());
        GuiChromeLayout.Rect historyBtn = layout.historyButtonRect();
        panelX = Math.max(0, historyBtn.getRight() - PANEL_WIDTH);
        panelY = GuiChromeLayout.TOP_BAR_HEIGHT;
    }

    private void refreshEntries() {
        entries = new ArrayList<>(historyStore.entries());
        Collections.reverse(entries);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        parentScreen.drawScreen(-1, -1, partialTicks);

        int visibleCount = Math.min(entries.size(), MAX_VISIBLE);
        int clearRowHeight = entries.isEmpty() ? 0 : ENTRY_HEIGHT;
        int panelHeight = visibleCount * ENTRY_HEIGHT + clearRowHeight + 4;
        if (entries.isEmpty()) {
            panelHeight = ENTRY_HEIGHT + 4;
        }

        Gui.drawRect(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, PANEL_BG);

        if (entries.isEmpty()) {
            fontRenderer.drawString("No history",
                panelX + 4, panelY + 4, TEXT_DIM);
            return;
        }

        int y = panelY + 2;
        for (int i = scrollOffset; i < Math.min(entries.size(), scrollOffset + MAX_VISIBLE); i++) {
            GuiHistoryEntry entry = entries.get(i);
            boolean clickable = entry.getSourceKey() != null;
            boolean hovered = mouseX >= panelX && mouseX < panelX + PANEL_WIDTH
                && mouseY >= y && mouseY < y + ENTRY_HEIGHT;

            if (hovered && clickable) {
                Gui.drawRect(panelX, y, panelX + PANEL_WIDTH, y + ENTRY_HEIGHT, ENTRY_HOVER);
            }

            String time = TIME_FORMAT.format(new Date(entry.getTimestamp()));
            String action = entry.getAction().name().substring(0, 1);
            String label = time + " [" + action + "] " + entry.getSessionTitle();
            String trimmed = trimToWidth(label, PANEL_WIDTH - 8);
            fontRenderer.drawString(trimmed, panelX + 4, y + 4,
                clickable ? TEXT_COLOR : TEXT_DIM);

            y += ENTRY_HEIGHT;
        }

        // Clear button
        boolean clearHovered = mouseX >= panelX && mouseX < panelX + PANEL_WIDTH
            && mouseY >= y && mouseY < y + ENTRY_HEIGHT;
        if (clearHovered) {
            Gui.drawRect(panelX, y, panelX + PANEL_WIDTH, y + ENTRY_HEIGHT, CLEAR_BG);
        }
        String clearText = "Clear History";
        fontRenderer.drawString(clearText,
            panelX + (PANEL_WIDTH - fontRenderer.getStringWidth(clearText)) / 2,
            y + 4, TEXT_COLOR);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        int y = panelY + 2;
        for (int i = scrollOffset; i < Math.min(entries.size(), scrollOffset + MAX_VISIBLE); i++) {
            GuiHistoryEntry entry = entries.get(i);
            boolean hovered = mouseX >= panelX && mouseX < panelX + PANEL_WIDTH
                && mouseY >= y && mouseY < y + ENTRY_HEIGHT;

            if (hovered && entry.getSourceKey() != null) {
                navigateToEntry(entry);
                return;
            }
            y += ENTRY_HEIGHT;
        }

        // Clear button
        int visibleCount = Math.min(entries.size(), MAX_VISIBLE);
        int clearY = panelY + 2 + visibleCount * ENTRY_HEIGHT;
        if (!entries.isEmpty() && mouseX >= panelX && mouseX < panelX + PANEL_WIDTH
            && mouseY >= clearY && mouseY < clearY + ENTRY_HEIGHT) {
            historyStore.clear();
            if (dataDir != null) historyStore.save(dataDir);
            refreshEntries();
            return;
        }

        Minecraft.getMinecraft().displayGuiScreen(parentScreen);
    }

    private void navigateToEntry(GuiHistoryEntry entry) {
        GuiSessionSourceKey key = entry.getSourceKey();
        if (!(key instanceof GuiSessionSourceKey.BlockKey)) {
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            return;
        }
        GuiSessionSource.BlockSource source = ((GuiSessionSourceKey.BlockKey) key).toBlockSource();

        for (GuiSession session : sessionManager.listAllSessions()) {
            if (source.equals(session.getSource())) {
                if (session.getScreen() instanceof StaleTabPlaceholderScreen) {
                    Minecraft.getMinecraft().displayGuiScreen(parentScreen);
                    if (restoreHandler != null) {
                        boolean restored = restoreHandler.requestRestore(session);
                        if (!restored) showRestoreToast(entry.getSessionTitle(), source);
                    }
                } else {
                    Minecraft.getMinecraft().displayGuiScreen(session.getScreen());
                    if (session.getScreen() instanceof net.minecraft.client.gui.inventory.GuiContainer
                            && restoreHandler != null
                            && !session.getId().equals(sessionManager.getLastServerWindowSessionId())) {
                        restoreHandler.requestSync(session);
                    }
                }
                return;
            }
        }

        GuiSession stale = sessionManager.registerStaleSession(entry.getSessionTitle(), source);
        Minecraft.getMinecraft().displayGuiScreen(parentScreen);
        if (restoreHandler != null) {
            boolean restored = restoreHandler.requestRestore(stale);
            if (!restored) {
                sessionManager.destroySession(stale.getId());
                showRestoreToast(entry.getSessionTitle(), source);
            }
        } else {
            sessionManager.destroySession(stale.getId());
        }
    }

    private void showRestoreToast(String title, GuiSessionSource.BlockSource source) {
        GuiRestoreFailedToast.show("Cannot reach " + title + " at "
            + source.getPos().getX() + ", " + source.getPos().getY() + ", " + source.getPos().getZ());
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int scroll = org.lwjgl.input.Mouse.getEventDWheel();
        if (scroll != 0) {
            if (scroll > 0 && scrollOffset > 0) scrollOffset--;
            if (scroll < 0 && scrollOffset < entries.size() - MAX_VISIBLE) scrollOffset++;
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (fontRenderer.getStringWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisW = fontRenderer.getStringWidth(ellipsis);
        while (text.length() > 1 && fontRenderer.getStringWidth(text) + ellipsisW > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
