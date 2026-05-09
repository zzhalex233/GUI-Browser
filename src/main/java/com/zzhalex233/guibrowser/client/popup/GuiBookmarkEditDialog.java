package com.zzhalex233.guibrowser.client.popup;

import com.zzhalex233.guibrowser.client.history.GuiBookmarkEntry;
import com.zzhalex233.guibrowser.client.history.GuiBookmarkStore;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiSessionSourceKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiBookmarkEditDialog extends GuiScreen {

    private static final int DIALOG_WIDTH = 240;
    private static final int DIALOG_HEIGHT = 116;
    private static final int BG_COLOR = 0xEE222222;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF888888;
    private static final int BTN_BG = 0xFF444444;
    private static final int BTN_HOVER = 0xFF666666;
    private static final int BTN_REMOVE_BG = 0xFF663333;
    private static final int BTN_REMOVE_HOVER = 0xFFAA4444;

    private final GuiScreen parentScreen;
    private final GuiSessionManager sessionManager;
    private final GuiBookmarkStore bookmarkStore;
    private final GuiSession session;
    @Nullable
    private final File dataDir;
    @Nullable
    private final GuiSessionSourceKey sourceKey;
    private final boolean alreadyBookmarked;

    private GuiTextField nameField;
    private int dialogX;
    private int dialogY;
    private List<String> folderPaths = new ArrayList<>();
    private int selectedFolderIndex;

    public GuiBookmarkEditDialog(GuiScreen parentScreen, GuiSessionManager sessionManager,
                                 GuiBookmarkStore bookmarkStore, GuiSession session,
                                 @Nullable File dataDir) {
        this.parentScreen = parentScreen;
        this.sessionManager = sessionManager;
        this.bookmarkStore = bookmarkStore;
        this.session = session;
        this.dataDir = dataDir;
        this.sourceKey = session.getSource() != null
            ? GuiSessionSourceKey.fromSource(session.getSource()) : null;
        this.alreadyBookmarked = sourceKey != null && bookmarkStore.isBookmarked(sourceKey);
    }

    @Override
    public void initGui() {
        super.initGui();
        dialogX = (width - DIALOG_WIDTH) / 2;
        dialogY = (height - DIALOG_HEIGHT) / 2;
        folderPaths = new ArrayList<>(bookmarkStore.folderPaths());
        selectedFolderIndex = 0;
        nameField = new GuiTextField(0, fontRenderer, dialogX + 8, dialogY + 24, DIALOG_WIDTH - 16, 14);
        nameField.setMaxStringLength(64);
        nameField.setFocused(true);
        if (alreadyBookmarked) {
            GuiBookmarkEntry entry = bookmarkStore.getBookmark(sourceKey);
            nameField.setText(entry != null ? entry.getTitle() : session.getTitle());
            selectedFolderIndex = indexOfFolder(entry == null ? "" : entry.getFolderPath());
        } else {
            nameField.setText(session.getTitle());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        Gui.drawRect(dialogX - 1, dialogY - 1, dialogX + DIALOG_WIDTH + 1, dialogY + DIALOG_HEIGHT + 1, BORDER_COLOR);
        Gui.drawRect(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + DIALOG_HEIGHT, BG_COLOR);

        String title = alreadyBookmarked ? tr("guibrowser.bookmark.edit.title") : tr("guibrowser.bookmark.add.title");
        fontRenderer.drawString(title, dialogX + 8, dialogY + 8, TEXT_COLOR);

        nameField.drawTextBox();
        drawFolderSelector(mouseX, mouseY);
        drawCoordinates();

        int btnY = dialogY + DIALOG_HEIGHT - 22;
        if (sourceKey != null) {
            drawButton(dialogX + 8, btnY, 58, tr("guibrowser.button.save"), mouseX, mouseY, BTN_BG, BTN_HOVER);
            if (alreadyBookmarked) {
                drawButton(dialogX + 72, btnY, 66, tr("guibrowser.button.remove"), mouseX, mouseY, BTN_REMOVE_BG, BTN_REMOVE_HOVER);
            }
        }
        drawButton(dialogX + DIALOG_WIDTH - 66, btnY, 58, tr("guibrowser.button.cancel"), mouseX, mouseY, BTN_BG, BTN_HOVER);
    }

    private void drawFolderSelector(int mouseX, int mouseY) {
        int y = dialogY + 44;
        fontRenderer.drawString(tr("guibrowser.bookmark.folder"), dialogX + 8, y + 4, TEXT_DIM);
        int boxX = dialogX + 68;
        int boxW = DIALOG_WIDTH - 76;
        boolean hovered = isInside(mouseX, mouseY, boxX, y, boxW, 16);
        Gui.drawRect(boxX, y, boxX + boxW, y + 16, hovered ? BTN_HOVER : BTN_BG);
        String path = folderPaths.isEmpty() ? "" : folderPaths.get(selectedFolderIndex);
        String label = path.isEmpty() ? tr("guibrowser.bookmark.folder.root") : path;
        fontRenderer.drawString(trimToWidth(label, boxW - 8), boxX + 4, y + 4, TEXT_COLOR);
    }

    private void drawCoordinates() {
        if (sourceKey instanceof GuiSessionSourceKey.BlockKey) {
            GuiSessionSourceKey.BlockKey bk = (GuiSessionSourceKey.BlockKey) sourceKey;
            String coords = tr("guibrowser.label.pos") + ": " + bk.getX() + ", " + bk.getY() + ", " + bk.getZ();
            fontRenderer.drawString(coords, dialogX + 8, dialogY + 66, TEXT_DIM);
        } else {
            fontRenderer.drawString(tr("guibrowser.bookmark.no_source"), dialogX + 8, dialogY + 66, TEXT_DIM);
        }
    }

    private void drawButton(int x, int y, int w, String text, int mx, int my, int bg, int hoverBg) {
        boolean hovered = isInside(mx, my, x, y, w, 16);
        Gui.drawRect(x, y, x + w, y + 16, hovered ? hoverBg : bg);
        int tx = x + (w - fontRenderer.getStringWidth(text)) / 2;
        fontRenderer.drawString(text, tx, y + 4, TEXT_COLOR);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;
        nameField.mouseClicked(mouseX, mouseY, mouseButton);

        int folderY = dialogY + 44;
        int folderX = dialogX + 68;
        int folderW = DIALOG_WIDTH - 76;
        if (isInside(mouseX, mouseY, folderX, folderY, folderW, 16) && !folderPaths.isEmpty()) {
            selectedFolderIndex = (selectedFolderIndex + 1) % folderPaths.size();
            return;
        }

        int btnY = dialogY + DIALOG_HEIGHT - 22;
        if (sourceKey != null && isInside(mouseX, mouseY, dialogX + 8, btnY, 58, 16)) {
            saveBookmark();
            return;
        }
        if (alreadyBookmarked && sourceKey != null && isInside(mouseX, mouseY, dialogX + 72, btnY, 66, 16)) {
            bookmarkStore.removeBookmark(sourceKey);
            if (dataDir != null) bookmarkStore.save(dataDir);
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            return;
        }
        if (isInside(mouseX, mouseY, dialogX + DIALOG_WIDTH - 66, btnY, 58, 16)) {
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            return;
        }
        if (mouseX < dialogX || mouseX > dialogX + DIALOG_WIDTH || mouseY < dialogY || mouseY > dialogY + DIALOG_HEIGHT) {
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
        }
    }

    private void saveBookmark() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = session.getTitle();
        String folderPath = folderPaths.isEmpty() ? "" : folderPaths.get(selectedFolderIndex);
        if (alreadyBookmarked) {
            bookmarkStore.updateTitle(sourceKey, name);
            if (sourceKey instanceof GuiSessionSourceKey.BlockKey) {
                bookmarkStore.moveBookmark((GuiSessionSourceKey.BlockKey) sourceKey, folderPath);
            }
        } else if (sourceKey instanceof GuiSessionSourceKey.BlockKey) {
            bookmarkStore.addBookmark(folderPath, (GuiSessionSourceKey.BlockKey) sourceKey, name, session.getScreen().getClass().getName());
        }
        if (dataDir != null) bookmarkStore.save(dataDir);
        Minecraft.getMinecraft().displayGuiScreen(parentScreen);
    }

    private int indexOfFolder(String folderPath) {
        for (int i = 0; i < folderPaths.size(); i++) {
            if (folderPaths.get(i).equals(folderPath)) {
                return i;
            }
        }
        return 0;
    }

    private boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            return;
        }
        nameField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        nameField.updateCursorCounter();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
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

    private static String tr(String key) {
        return I18n.format(key);
    }
}
