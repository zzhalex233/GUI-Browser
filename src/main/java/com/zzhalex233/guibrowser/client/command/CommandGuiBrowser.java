package com.zzhalex233.guibrowser.client.command;

import com.zzhalex233.guibrowser.client.history.GuiHistoryEntry;
import com.zzhalex233.guibrowser.client.history.GuiHistoryStore;
import com.zzhalex233.guibrowser.client.runtime.GuiBrowserRuntime;
import com.zzhalex233.guibrowser.client.session.GuiSession;
import com.zzhalex233.guibrowser.client.session.GuiSessionManager;
import com.zzhalex233.guibrowser.client.session.GuiSessionSource;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.List;

public class CommandGuiBrowser extends CommandBase {

    @Override
    public String getName() {
        return "guibrowser";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/guibrowser <tabs|clear|history|debug>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(getUsage(sender)));
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "tabs":
                handleTabs(sender);
                break;
            case "clear":
                handleClear(sender);
                break;
            case "history":
                handleHistory(sender);
                break;
            case "debug":
                handleDebug(sender);
                break;
            default:
                sender.sendMessage(new TextComponentString("Unknown subcommand: " + sub));
                break;
        }
    }

    private void handleTabs(ICommandSender sender) {
        GuiSessionManager manager = GuiBrowserRuntime.getInstance().getSessionManager();
        List<GuiSession> sessions = manager.listAllSessions();
        if (sessions.isEmpty()) {
            sender.sendMessage(new TextComponentString("No active tab sessions."));
            return;
        }
        sender.sendMessage(new TextComponentString("Tab sessions (" + sessions.size() + "):"));
        for (GuiSession session : sessions) {
            String status = session.isForeground() ? "[active]" : session.isHidden() ? "[hidden]" : "[cached]";
            sender.sendMessage(new TextComponentString("  " + session.getTitle() + " " + status));
        }
    }

    private void handleClear(ICommandSender sender) {
        GuiSessionManager manager = GuiBrowserRuntime.getInstance().getSessionManager();
        int count = manager.listAllSessions().size();
        manager.clearForWorldUnload();
        sender.sendMessage(new TextComponentString("Cleared " + count + " cached sessions."));
    }

    private void handleHistory(ICommandSender sender) {
        GuiHistoryStore store = GuiBrowserRuntime.getInstance().getSessionManager().getHistoryStore();
        if (store == null) {
            sender.sendMessage(new TextComponentString("History tracking is not enabled."));
            return;
        }
        List<GuiHistoryEntry> entries = store.entries();
        sender.sendMessage(new TextComponentString("History entries: " + entries.size()));
        int start = Math.max(0, entries.size() - 10);
        for (int i = start; i < entries.size(); i++) {
            GuiHistoryEntry entry = entries.get(i);
            sender.sendMessage(new TextComponentString(
                    "  " + entry.getSessionTitle() + " - " + entry.getAction().name().toLowerCase()));
        }
    }

    private void handleDebug(ICommandSender sender) {
        GuiBrowserRuntime runtime = GuiBrowserRuntime.getInstance();
        GuiSessionManager manager = runtime.getSessionManager();
        GuiSession foreground = manager.getForegroundSession();

        sender.sendMessage(new TextComponentString("--- GUI Browser Debug ---"));
        sender.sendMessage(new TextComponentString("Cache mode: " + runtime.getConfig().getContainerCacheMode().name()));
        sender.sendMessage(new TextComponentString("Total sessions: " + manager.listAllSessions().size()));
        sender.sendMessage(new TextComponentString("Visible tabs: " + manager.listVisibleTabs().size()));
        sender.sendMessage(new TextComponentString("Foreground: " + (foreground != null ? foreground.getTitle() : "none")));
        GuiHistoryStore historyStore = manager.getHistoryStore();
        sender.sendMessage(new TextComponentString("History entries: " + (historyStore != null ? historyStore.size() : "disabled")));

        for (GuiSession session : manager.listAllSessions()) {
            String sourceStr = formatSource(session.getSource());
            String staleStr = session.isStale() ? " [stale]" : "";
            sender.sendMessage(new TextComponentString("  " + session.getTitle() + " source=" + sourceStr + staleStr));
        }
    }

    private static String formatSource(@javax.annotation.Nullable GuiSessionSource source) {
        if (source == null) {
            return "none";
        }
        if (source instanceof GuiSessionSource.BlockSource) {
            return ((GuiSessionSource.BlockSource) source).getPos().toString();
        }
        if (source instanceof GuiSessionSource.EntitySource) {
            return "entity#" + ((GuiSessionSource.EntitySource) source).getEntityId();
        }
        return source.toString();
    }
}
