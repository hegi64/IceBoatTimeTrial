package com.hegi64.iceBoatTimeTrial.command.sub;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.gui.GuiContext;
import com.hegi64.iceBoatTimeTrial.gui.GuiSessionService;
import com.hegi64.iceBoatTimeTrial.gui.menu.SettingsMenu;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SettingsSubCommand implements SubCommand {
    private final PluginConfig config;
    private final SettingsMenu settingsMenu;
    private final GuiSessionService sessions;

    public SettingsSubCommand(PluginConfig config, SettingsMenu settingsMenu, GuiSessionService sessions) {
        this.config = config;
        this.settingsMenu = settingsMenu;
        this.sessions = sessions;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly players can open settings.");
            return true;
        }
        settingsMenu.open(player, sessions, new GuiContext());
        return true;
    }

    @Override
    public String getName() {
        return "settings";
    }

    @Override
    public String getDescription() {
        return "/ibt settings";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.PLAYER);
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }
}

