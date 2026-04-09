package com.hegi64.iceBoatTimeTrial.command.sub;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.gui.GuiContext;
import com.hegi64.iceBoatTimeTrial.gui.GuiSessionService;
import com.hegi64.iceBoatTimeTrial.gui.menu.TrackBrowserMenu;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TracksSubCommand implements SubCommand {
    private final PluginConfig config;
    private final TrackBrowserMenu browserMenu;
    private final GuiSessionService sessions;

    public TracksSubCommand(PluginConfig config, TrackBrowserMenu browserMenu, GuiSessionService sessions) {
        this.config = config;
        this.browserMenu = browserMenu;
        this.sessions = sessions;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly players can open the track browser.");
            return true;
        }
        browserMenu.open(player, sessions, new GuiContext().put("page", 0));
        return true;
    }

    @Override
    public String getName() {
        return "tracks";
    }

    @Override
    public String getDescription() {
        return "/ibt tracks";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.PLAYER);
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }
}

