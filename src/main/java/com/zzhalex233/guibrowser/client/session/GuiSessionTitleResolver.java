package com.zzhalex233.guibrowser.client.session;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

public final class GuiSessionTitleResolver {
    private GuiSessionTitleResolver() {
    }

    public static String resolve(GuiScreen screen, String explicitTitle) {
        return resolve(screen, explicitTitle, null);
    }

    public static String resolve(GuiScreen screen, String explicitTitle, GuiSessionSource source) {
        String blockTitle = resolveBlockTitle(source);
        if (blockTitle != null) {
            return blockTitle;
        }
        if (explicitTitle != null) {
            String trimmedTitle = explicitTitle.trim();
            if (!trimmedTitle.isEmpty()) {
                return trimmedTitle;
            }
        }
        if (screen != null) {
            String simpleName = screen.getClass().getSimpleName();
            if (simpleName != null && !simpleName.trim().isEmpty()) {
                return simpleName.trim();
            }
        }
        return "GUI";
    }

    private static String resolveBlockTitle(GuiSessionSource source) {
        if (!(source instanceof GuiSessionSource.BlockSource)) {
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null) {
            return null;
        }
        GuiSessionSource.BlockSource blockSource = (GuiSessionSource.BlockSource) source;
        if (mc.player == null || mc.player.dimension != blockSource.getDimensionId()) {
            return null;
        }
        if (!mc.world.isBlockLoaded(blockSource.getPos())) {
            return null;
        }
        IBlockState state = mc.world.getBlockState(blockSource.getPos());
        ItemStack stack = new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state));
        if (!stack.isEmpty()) {
            String displayName = stack.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName.trim();
            }
        }
        String localizedName = state.getBlock().getLocalizedName();
        return localizedName == null || localizedName.trim().isEmpty() ? null : localizedName.trim();
    }
}
