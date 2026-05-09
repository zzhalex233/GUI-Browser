package com.zzhalex233.guibrowser.client.popup;

import com.zzhalex233.guibrowser.client.history.GuiBookmarkEntry;
import com.zzhalex233.guibrowser.client.history.GuiBookmarkFolder;
import com.zzhalex233.guibrowser.client.history.GuiBookmarkStore;
import com.zzhalex233.guibrowser.client.session.GuiBlockOpenAvailability;
import com.zzhalex233.guibrowser.client.session.GuiRestoreRequester;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiBookmarkPanel extends GuiScreen {

    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 280;
    private static final int TREE_WIDTH = 154;
    private static final int ROW_HEIGHT = 18;
    private static final int TOOLBAR_HEIGHT = 28;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF888888;
    private static final int PANEL_BG = 0xF2222222;
    private static final int TREE_BG = 0xFF161616;
    private static final int LIST_BG = 0xFF242424;
    private static final int ROW_HOVER = 0x44FFFFFF;
    private static final int BUTTON_BG = 0xFF3A3A3A;
    private static final int BUTTON_HOVER = 0xFF555555;
    private static final int BUTTON_DANGER = 0xFF663333;
    private static final int BUTTON_DANGER_HOVER = 0xFFAA4444;

    private final GuiScreen parentScreen;
    private final GuiBookmarkStore bookmarkStore;
    private final GuiSessionManager sessionManager;
    @Nullable
    private final GuiRestoreRequester restoreHandler;
    @Nullable
    private final File dataDir;

    private int panelX;
    private int panelY;
    private String selectedFolder = "";
    private List<FolderRow> folderRows = new ArrayList<>();
    private List<GuiBookmarkEntry> visibleEntries = new ArrayList<>();
    private GuiTextField newFolderField;
    private boolean creatingFolder;

    public GuiBookmarkPanel(GuiScreen parentScreen, GuiBookmarkStore bookmarkStore,
                            GuiSessionManager sessionManager,
                            @Nullable GuiRestoreRequester restoreHandler,
                            @Nullable File dataDir) {
        this.parentScreen = parentScreen;
        this.bookmarkStore = bookmarkStore;
        this.sessionManager = sessionManager;
        this.restoreHandler = restoreHandler;
        this.dataDir = dataDir;
    }

    @Override
    public void initGui() {
        super.initGui();
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        newFolderField = new GuiTextField(1, fontRenderer, panelX + TREE_WIDTH + 118, panelY + 8, 120, 14);
        newFolderField.setMaxStringLength(48);
        refreshRows();
    }

    private void refreshRows() {
        folderRows = new ArrayList<>();
        folderRows.add(new FolderRow("", tr("guibrowser.bookmark.folder.root"), 0));
        collectFolderRows(bookmarkStore.getRootFolder(), "", 0);
        GuiBookmarkFolder folder = bookmarkStore.getRootFolder().findFolder(selectedFolder);
        visibleEntries = folder == null ? new ArrayList<GuiBookmarkEntry>() : new ArrayList<>(folder.getEntries());
    }

    private void collectFolderRows(GuiBookmarkFolder folder, String prefix, int depth) {
        for (GuiBookmarkFolder child : folder.getChildren()) {
            String path = prefix.isEmpty() ? child.getName() : prefix + "/" + child.getName();
            folderRows.add(new FolderRow(path, child.getName(), depth + 1));
            collectFolderRows(child, path, depth + 1);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GlStateManager.disableDepth();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 500.0F);

        Gui.drawRect(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, 0xFF555555);
        Gui.drawRect(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_BG);
        Gui.drawRect(panelX, panelY + TOOLBAR_HEIGHT, panelX + TREE_WIDTH, panelY + PANEL_HEIGHT, TREE_BG);
        Gui.drawRect(panelX + TREE_WIDTH, panelY + TOOLBAR_HEIGHT, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, LIST_BG);

        drawToolbar(mouseX, mouseY);
        drawTree(mouseX, mouseY);
        drawList(mouseX, mouseY);
        GuiRestoreFailedToast.draw(width, fontRenderer, panelY + 6);

        GlStateManager.popMatrix();
        GlStateManager.enableDepth();
    }

    private void drawToolbar(int mouseX, int mouseY) {
        fontRenderer.drawString(tr("guibrowser.bookmark.manager.title"), panelX + 8, panelY + 10, TEXT_COLOR);
        int buttonX = panelX + TREE_WIDTH + 8;
        drawButton(buttonX, panelY + 6, 100, tr("guibrowser.bookmark.folder.new"), mouseX, mouseY, BUTTON_BG, BUTTON_HOVER);
        if (creatingFolder) {
            newFolderField.drawTextBox();
            drawButton(panelX + TREE_WIDTH + 246, panelY + 6, 46, tr("guibrowser.button.save"), mouseX, mouseY, BUTTON_BG, BUTTON_HOVER);
        }
    }

    private void drawTree(int mouseX, int mouseY) {
        int y = panelY + TOOLBAR_HEIGHT + 6;
        for (FolderRow row : folderRows) {
            boolean selected = selectedFolder.equals(row.path);
            boolean hovered = mouseX >= panelX && mouseX < panelX + TREE_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (selected || hovered) {
                Gui.drawRect(panelX, y, panelX + TREE_WIDTH, y + ROW_HEIGHT, ROW_HOVER);
            }
            fontRenderer.drawString(trimToWidth(row.label, TREE_WIDTH - 12 - row.depth * 10),
                panelX + 6 + row.depth * 10, y + 4, TEXT_COLOR);
            y += ROW_HEIGHT;
        }
    }

    private void drawList(int mouseX, int mouseY) {
        int x = panelX + TREE_WIDTH + 8;
        int y = panelY + TOOLBAR_HEIGHT + 8;
        if (visibleEntries.isEmpty()) {
            fontRenderer.drawString(tr("guibrowser.bookmark.empty"), x, y, TEXT_DIM);
            return;
        }

        for (GuiBookmarkEntry entry : visibleEntries) {
            boolean hovered = mouseX >= x && mouseX < panelX + PANEL_WIDTH - 8 && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (hovered) {
                Gui.drawRect(x, y, panelX + PANEL_WIDTH - 8, y + ROW_HEIGHT, ROW_HOVER);
            }
            fontRenderer.drawString(trimToWidth(entry.getDisplayLabel(), PANEL_WIDTH - TREE_WIDTH - 98), x + 4, y + 4, TEXT_COLOR);
            int openX = panelX + PANEL_WIDTH - 80;
            int removeX = panelX + PANEL_WIDTH - 42;
            drawButton(openX, y + 1, 34, tr("guibrowser.button.open"), mouseX, mouseY, BUTTON_BG, BUTTON_HOVER);
            drawButton(removeX, y + 1, 34, tr("guibrowser.button.remove.short"), mouseX, mouseY, BUTTON_DANGER, BUTTON_DANGER_HOVER);
            y += ROW_HEIGHT;
        }
    }

    private void drawButton(int x, int y, int w, String text, int mx, int my, int bg, int hoverBg) {
        boolean hovered = isInside(mx, my, x, y, w, 16);
        Gui.drawRect(x, y, x + w, y + 16, hovered ? hoverBg : bg);
        fontRenderer.drawString(trimToWidth(text, w - 4), x + 2, y + 4, TEXT_COLOR);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;
        if (creatingFolder) {
            newFolderField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        int newFolderX = panelX + TREE_WIDTH + 8;
        if (isInside(mouseX, mouseY, newFolderX, panelY + 6, 100, 16)) {
            creatingFolder = true;
            newFolderField.setFocused(true);
            return;
        }
        if (creatingFolder && isInside(mouseX, mouseY, panelX + TREE_WIDTH + 246, panelY + 6, 46, 16)) {
            createFolderFromField();
            return;
        }

        int treeY = panelY + TOOLBAR_HEIGHT + 6;
        for (FolderRow row : folderRows) {
            if (isInside(mouseX, mouseY, panelX, treeY, TREE_WIDTH, ROW_HEIGHT)) {
                selectedFolder = row.path;
                refreshRows();
                return;
            }
            treeY += ROW_HEIGHT;
        }

        int listY = panelY + TOOLBAR_HEIGHT + 8;
        for (GuiBookmarkEntry entry : new ArrayList<>(visibleEntries)) {
            int openX = panelX + PANEL_WIDTH - 80;
            int removeX = panelX + PANEL_WIDTH - 42;
            if (isInside(mouseX, mouseY, openX, listY + 1, 34, 16)) {
                openBookmark(entry);
                return;
            }
            if (isInside(mouseX, mouseY, removeX, listY + 1, 34, 16)) {
                bookmarkStore.remove(entry.getSourceKey());
                if (dataDir != null) bookmarkStore.save(dataDir);
                refreshRows();
                return;
            }
            if (isInside(mouseX, mouseY, panelX + TREE_WIDTH + 8, listY, PANEL_WIDTH - TREE_WIDTH - 92, ROW_HEIGHT)) {
                openBookmark(entry);
                return;
            }
            listY += ROW_HEIGHT;
        }

        if (mouseX < panelX || mouseX > panelX + PANEL_WIDTH || mouseY < panelY || mouseY > panelY + PANEL_HEIGHT) {
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
        }
    }

    private void createFolderFromField() {
        String name = newFolderField.getText().trim();
        if (!name.isEmpty()) {
            String path = selectedFolder.isEmpty() ? name : selectedFolder + "/" + name;
            bookmarkStore.addFolderPath(path);
            selectedFolder = path;
            if (dataDir != null) bookmarkStore.save(dataDir);
        }
        creatingFolder = false;
        newFolderField.setText("");
        refreshRows();
    }

    private void openBookmark(GuiBookmarkEntry entry) {
        GuiSessionSourceKey.BlockKey key = entry.getSourceKey();
        GuiSessionSource.BlockSource source = key.toBlockSource();

        Minecraft mc = Minecraft.getMinecraft();
        int currentDimension = mc.player == null ? Integer.MIN_VALUE : mc.player.dimension;
        GuiBlockOpenAvailability.Result availability =
            GuiBlockOpenAvailability.check(mc.world, currentDimension, key);
        if (availability == GuiBlockOpenAvailability.Result.WRONG_DIMENSION) {
            GuiRestoreFailedToast.show(tr("guibrowser.bookmark.wrong_dimension"));
            return;
        }
        if (availability == GuiBlockOpenAvailability.Result.UNREACHABLE) {
            showUnreachableToast(entry.getTitle(), key);
            return;
        }

        for (GuiSession session : sessionManager.listAllSessions()) {
            if (source.equals(session.getSource())) {
                mc.displayGuiScreen(session.getScreen());
                return;
            }
        }

        if (restoreHandler == null || !restoreHandler.requestRestore(entry.getTitle(), source)) {
            showUnreachableToast(entry.getTitle(), key);
        }
    }

    private void showUnreachableToast(String title, GuiSessionSourceKey.BlockKey key) {
        GuiRestoreFailedToast.show(I18n.format("guibrowser.restore.unreachable", title,
            key.getX(), key.getY(), key.getZ()));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            return;
        }
        if (creatingFolder) {
            if (keyCode == 28) {
                createFolderFromField();
                return;
            }
            newFolderField.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        if (creatingFolder) {
            newFolderField.updateCursorCounter();
        }
    }

    private boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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

    private static String tr(String key) {
        return I18n.format(key);
    }

    private static final class FolderRow {
        private final String path;
        private final String label;
        private final int depth;

        private FolderRow(String path, String label, int depth) {
            this.path = path;
            this.label = label;
            this.depth = depth;
        }
    }
}
