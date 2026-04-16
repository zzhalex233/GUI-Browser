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

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

public class GuiBookmarkEditDialog extends GuiScreen {

    private static final int DIALOG_WIDTH = 200;
    private static final int DIALOG_HEIGHT = 90;
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
        nameField = new GuiTextField(0, fontRenderer, dialogX + 6, dialogY + 22, DIALOG_WIDTH - 12, 14);
        nameField.setMaxStringLength(64);
        nameField.setFocused(true);
        if (alreadyBookmarked) {
            GuiBookmarkEntry entry = bookmarkStore.getBookmark(sourceKey);
            nameField.setText(entry != null ? entry.getSessionTitle() : session.getTitle());
        } else {
            nameField.setText(session.getTitle());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Dialog background
        Gui.drawRect(dialogX - 1, dialogY - 1, dialogX + DIALOG_WIDTH + 1, dialogY + DIALOG_HEIGHT + 1, BORDER_COLOR);
        Gui.drawRect(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + DIALOG_HEIGHT, BG_COLOR);

        // Title
        String title = alreadyBookmarked ? "Edit Bookmark" : "Add Bookmark";
        fontRenderer.drawString(title, dialogX + 6, dialogY + 6, TEXT_COLOR);

        // Name field
        nameField.drawTextBox();

        // Coordinates
        if (sourceKey instanceof GuiSessionSourceKey.BlockKey) {
            GuiSessionSourceKey.BlockKey bk = (GuiSessionSourceKey.BlockKey) sourceKey;
            String coords = "Pos: " + bk.getX() + ", " + bk.getY() + ", " + bk.getZ();
            fontRenderer.drawString(coords, dialogX + 6, dialogY + 42, TEXT_DIM);
        } else {
            fontRenderer.drawString("No block source", dialogX + 6, dialogY + 42, TEXT_DIM);
        }

        // Buttons row
        int btnY = dialogY + DIALOG_HEIGHT - 20;
        if (sourceKey != null) {
            // Save button
            drawButton(dialogX + 6, btnY, 55, "Save", mouseX, mouseY, BTN_BG, BTN_HOVER);
            if (alreadyBookmarked) {
                // Remove button
                drawButton(dialogX + 66, btnY, 65, "Remove", mouseX, mouseY, BTN_REMOVE_BG, BTN_REMOVE_HOVER);
            }
        }
        // Done button
        drawButton(dialogX + DIALOG_WIDTH - 50, btnY, 44, "Done", mouseX, mouseY, BTN_BG, BTN_HOVER);
    }

    private void drawButton(int x, int y, int w, String text, int mx, int my, int bg, int hoverBg) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + 16;
        Gui.drawRect(x, y, x + w, y + 16, hovered ? hoverBg : bg);
        int tx = x + (w - fontRenderer.getStringWidth(text)) / 2;
        fontRenderer.drawString(text, tx, y + 4, TEXT_COLOR);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;
        nameField.mouseClicked(mouseX, mouseY, mouseButton);

        int btnY = dialogY + DIALOG_HEIGHT - 20;
        if (sourceKey != null && isInside(mouseX, mouseY, dialogX + 6, btnY, 55, 16)) {
            // Save
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = session.getTitle();
            if (alreadyBookmarked) {
                bookmarkStore.updateTitle(sourceKey, name);
            } else {
                bookmarkStore.addBookmark(sourceKey, name, session.getScreen().getClass().getName());
            }
            if (dataDir != null) bookmarkStore.save(dataDir);
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            return;
        }
        if (alreadyBookmarked && sourceKey != null && isInside(mouseX, mouseY, dialogX + 66, btnY, 65, 16)) {
            // Remove
            bookmarkStore.removeBookmark(sourceKey);
            if (dataDir != null) bookmarkStore.save(dataDir);
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            return;
        }
        if (isInside(mouseX, mouseY, dialogX + DIALOG_WIDTH - 50, btnY, 44, 16)) {
            // Done
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            return;
        }
        // Click outside dialog -> close
        if (mouseX < dialogX || mouseX > dialogX + DIALOG_WIDTH || mouseY < dialogY || mouseY > dialogY + DIALOG_HEIGHT) {
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
        }
    }

    private boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
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
}
