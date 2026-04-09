package com.zzhalex233.guibrowser.client.command;

import com.zzhalex233.guibrowser.client.browser.BrowserManager;
import com.zzhalex233.guibrowser.client.browser.BrowserState;
import com.zzhalex233.guibrowser.client.gui.BrowserRootGui;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandGuiBrowser extends CommandBase {
    private static final List<String> SUBCOMMANDS = Arrays.asList("open", "close", "toggle", "state");

    @Override
    public String getName() {
        return "guibrowser";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/guibrowser <open|close|toggle|state>";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new CommandException(getUsage(sender));
        }

        BrowserManager manager = BrowserManager.getInstance();
        String subcommand = args[0].toLowerCase();
        if ("state".equals(subcommand)) {
            sender.sendMessage(new TextComponentString("GUI Browser state: " + manager.getState().name()));
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.world == null) {
            sender.sendMessage(new TextComponentString("GUI Browser can only be used while in-world."));
            return;
        }

        switch (subcommand) {
            case "open":
                openBrowser(minecraft, manager);
                sender.sendMessage(new TextComponentString("GUI Browser opened."));
                break;
            case "close":
                manager.closeBrowser();
                if (minecraft.currentScreen instanceof BrowserRootGui) {
                    minecraft.displayGuiScreen(null);
                }
                sender.sendMessage(new TextComponentString("GUI Browser closed."));
                break;
            case "toggle":
                toggleBrowser(minecraft, manager);
                sender.sendMessage(new TextComponentString("GUI Browser state: " + manager.getState().name()));
                break;
            default:
                throw new CommandException(getUsage(sender));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    private static void openBrowser(Minecraft minecraft, BrowserManager manager) {
        manager.openEmptyBrowser();
        minecraft.displayGuiScreen(new BrowserRootGui(manager));
    }

    private static void toggleBrowser(Minecraft minecraft, BrowserManager manager) {
        if (manager.getState() == BrowserState.CLOSED) {
            openBrowser(minecraft, manager);
            return;
        }
        if (minecraft.currentScreen instanceof BrowserRootGui) {
            manager.closeBrowser();
            minecraft.displayGuiScreen(null);
            return;
        }
        openBrowser(minecraft, manager);
    }
}
