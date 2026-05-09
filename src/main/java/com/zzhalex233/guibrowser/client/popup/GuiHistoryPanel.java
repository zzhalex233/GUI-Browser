package com.zzhalex233.guibrowser.client.popup;

import com.zzhalex233.guibrowser.client.chrome.GuiChromeLayout;
import com.zzhalex233.guibrowser.client.history.GuiHistoryEntry;
import com.zzhalex233.guibrowser.client.history.GuiHistoryStore;
import com.zzhalex233.guibrowser.client.session.GuiBlockOpenAvailability;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;
import com.zzhalex233.guibrowser.client.session.GuiRestoreRequester;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GuiHistoryPanel extends GuiScreen {

    private static final int PANEL_WIDTH = 360;
    private static final int LIST_WIDTH = 180;
    private static final int ROW_HEIGHT = 18;
    private static final int MAX_VISIBLE = 14;
    private static final int PANEL_BG = 0xEE1A1A1A;
    private static final int LEFT_BG = 0xFF141414;
    private static final int RIGHT_BG = 0xFF1F1F1F;
    private static final int ROW_HOVER = 0x44FFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF888888;
    private static final int ACTION_BG = 0xFF333333;
    private static final int ACTION_HOVER = 0xFF555555;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    private final GuiScreen parentScreen;
    private final GuiHistoryStore historyStore;
    private final GuiSessionManager sessionManager;
    @Nullable
    private final GuiRestoreRequester restoreHandler;
    @Nullable
    private final File dataDir;
    private int panelX;
    private int panelY;
    private int scrollOffset;
    private List<GuiHistoryEntry> entries = new ArrayList<>();
    private GuiHistoryEntry selectedEntry;

    public GuiHistoryPanel(GuiScreen parentScreen, GuiHistoryStore historyStore,
                           GuiSessionManager sessionManager,
                           @Nullable GuiRestoreRequester restoreHandler,
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
        selectedEntry = entries.isEmpty() ? null : entries.get(0);
    }

    private void refreshEntries() {
        int dimensionId = Minecraft.getMinecraft().player == null ? Integer.MIN_VALUE : Minecraft.getMinecraft().player.dimension;
        entries = new ArrayList<>(historyStore.entriesForDimension(dimensionId));
        for (int i = 0, j = entries.size() - 1; i < j; i++, j--) {
            GuiHistoryEntry tmp = entries.get(i);
            entries.set(i, entries.get(j));
            entries.set(j, tmp);
        }
        if (selectedEntry == null && !entries.isEmpty()) {
            selectedEntry = entries.get(0);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GlStateManager.disableDepth();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 500.0F);

        int panelHeight = Math.max(180, Math.min(ROW_HEIGHT * MAX_VISIBLE + 8, (entries.size() + 1) * ROW_HEIGHT + 8));
        Gui.drawRect(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + panelHeight + 1, 0xFF555555);
        Gui.drawRect(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, PANEL_BG);

        Gui.drawRect(panelX, panelY, panelX + LIST_WIDTH, panelY + panelHeight, LEFT_BG);
        Gui.drawRect(panelX + LIST_WIDTH, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, RIGHT_BG);

        drawList(mouseX, mouseY);
        drawDetails(mouseX, mouseY);
        GuiRestoreFailedToast.draw(width, fontRenderer, panelY + 6);

        GlStateManager.popMatrix();
        GlStateManager.enableDepth();
    }

    private void drawList(int mouseX, int mouseY) {
        if (entries.isEmpty()) {
            fontRenderer.drawString(I18n.format("guibrowser.history.empty"), panelX + 8, panelY + 8, TEXT_DIM);
            return;
        }

        int y = panelY + 6;
        for (int i = scrollOffset; i < Math.min(entries.size(), scrollOffset + MAX_VISIBLE); i++) {
            GuiHistoryEntry entry = entries.get(i);
            boolean selected = entry == selectedEntry;
            boolean hovered = mouseX >= panelX && mouseX < panelX + LIST_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (selected || hovered) {
                Gui.drawRect(panelX, y, panelX + LIST_WIDTH, y + ROW_HEIGHT, ROW_HOVER);
            }
            String title = trimToWidth(entry.getSessionTitle(), LIST_WIDTH - 12);
            fontRenderer.drawString(title, panelX + 6, y + 4, TEXT_COLOR);
            y += ROW_HEIGHT;
        }
    }

    private void drawDetails(int mouseX, int mouseY) {
        int x = panelX + LIST_WIDTH + 8;
        int y = panelY + 8;
        if (selectedEntry == null) {
            fontRenderer.drawString(I18n.format("guibrowser.history.select_row"), x, y, TEXT_DIM);
            return;
        }

        GuiHistoryEntry entry = selectedEntry;
        GuiSessionSourceKey.BlockKey key = entry.getSourceKey();
        fontRenderer.drawString(entry.getSessionTitle(), x, y, TEXT_COLOR);
        fontRenderer.drawString(I18n.format("guibrowser.history.pos", key.getX(), key.getY(), key.getZ()), x, y + 14, TEXT_DIM);
        fontRenderer.drawString(I18n.format("guibrowser.history.dim", key.getDimensionId()), x, y + 28, TEXT_DIM);
        fontRenderer.drawString(I18n.format("guibrowser.history.closed", TIME_FORMAT.format(new Date(entry.getClosedAt()))), x, y + 42, TEXT_DIM);
        fontRenderer.drawString(I18n.format("guibrowser.history.count", entry.getCloseCount()), x, y + 56, TEXT_DIM);

        int actionY = y + 78;
        boolean hovered = mouseX >= x && mouseX < x + 120 && mouseY >= actionY && mouseY < actionY + 18;
        Gui.drawRect(x, actionY, x + 120, actionY + 18, hovered ? ACTION_HOVER : ACTION_BG);
        fontRenderer.drawString(I18n.format("guibrowser.button.open"), x + 46, actionY + 5, TEXT_COLOR);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        if (mouseX >= panelX && mouseX < panelX + LIST_WIDTH) {
            int y = panelY + 6;
            for (int i = scrollOffset; i < Math.min(entries.size(), scrollOffset + MAX_VISIBLE); i++) {
                GuiHistoryEntry entry = entries.get(i);
                if (mouseY >= y && mouseY < y + ROW_HEIGHT) {
                    selectedEntry = entry;
                    return;
                }
                y += ROW_HEIGHT;
            }
        }

        if (selectedEntry != null) {
            int x = panelX + LIST_WIDTH + 8;
            int actionY = panelY + 86;
            if (mouseX >= x && mouseX < x + 120 && mouseY >= actionY && mouseY < actionY + 18) {
                navigateToEntry(selectedEntry);
                return;
            }
        }

        Minecraft.getMinecraft().displayGuiScreen(parentScreen);
    }

    private void navigateToEntry(GuiHistoryEntry entry) {
        GuiSessionSourceKey.BlockKey key = entry.getSourceKey();
        GuiSessionSource.BlockSource source = key.toBlockSource();
        Minecraft mc = Minecraft.getMinecraft();
        int currentDimension = mc.player == null ? Integer.MIN_VALUE : mc.player.dimension;
        GuiBlockOpenAvailability.Result availability =
            GuiBlockOpenAvailability.check(mc.world, currentDimension, key);
        if (availability == GuiBlockOpenAvailability.Result.WRONG_DIMENSION) {
            GuiRestoreFailedToast.show(I18n.format("guibrowser.bookmark.wrong_dimension"));
            return;
        }
        if (availability == GuiBlockOpenAvailability.Result.UNREACHABLE) {
            showRestoreToast(entry.getSessionTitle(), source);
            return;
        }

        for (GuiSession session : sessionManager.listAllSessions()) {
            if (source.equals(session.getSource())) {
                mc.displayGuiScreen(session.getScreen());
                return;
            }
        }

        if (restoreHandler != null) {
            boolean restored = restoreHandler.requestRestore(entry.getSessionTitle(), source);
            if (!restored) {
                showRestoreToast(entry.getSessionTitle(), source);
            }
        } else {
            showRestoreToast(entry.getSessionTitle(), source);
        }
    }

    private void showRestoreToast(String title, GuiSessionSource.BlockSource source) {
        GuiRestoreFailedToast.show(I18n.format("guibrowser.restore.unreachable", title,
            source.getPos().getX(), source.getPos().getY(), source.getPos().getZ()));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
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
