package com.hegi64.iceBoatTimeTrial.command.sub.editor;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatusEditorSubCommand implements SubCommand {
    private final EditorCommandContext context;

    public StatusEditorSubCommand(EditorCommandContext context) {
        this.context = context;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.send(sender, "&cOnly players can use this command.");
            return true;
        }

        try {
            return context.summary(player);
        } catch (IllegalStateException exception) {
            context.send(player, "&c" + exception.getMessage());
            return true;
        }
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public String getDescription() {
        return "/ite status";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.EDITOR_USE);
    }
}

