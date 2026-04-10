package com.zzhalex233.guibrowser.client.command;

import com.zzhalex233.guibrowser.client.browser.BrowserShellController;
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

    private final BrowserShellController controller;

    public CommandGuiBrowser(BrowserShellController controller) {
        this.controller = controller;
    }

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

        String subcommand = args[0].toLowerCase();
        if ("state".equals(subcommand)) {
            sender.sendMessage(new TextComponentString("GUI Browser state: " + controller.getManager().getState().name()));
            return;
        }

        switch (subcommand) {
            case "open":
                if (controller.requestBrowserOpenFromCommand()) {
                    sender.sendMessage(new TextComponentString("GUI Browser opened."));
                } else {
                    sender.sendMessage(new TextComponentString("GUI Browser can only be used while in-world."));
                }
                break;
            case "close":
                controller.closeBrowser();
                sender.sendMessage(new TextComponentString("GUI Browser closed."));
                break;
            case "toggle":
                if (controller.toggleBrowser()) {
                    sender.sendMessage(new TextComponentString("GUI Browser state: " + controller.getManager().getState().name()));
                } else {
                    sender.sendMessage(new TextComponentString("GUI Browser can only be used while in-world."));
                }
                break;
            default:
                throw new CommandException(getUsage(sender));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}