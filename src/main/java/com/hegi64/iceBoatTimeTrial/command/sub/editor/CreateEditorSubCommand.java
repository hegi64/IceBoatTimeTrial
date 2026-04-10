package com.hegi64.iceBoatTimeTrial.command.sub.editor;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class CreateEditorSubCommand implements SubCommand {
    private final EditorCommandContext context;

    public CreateEditorSubCommand(EditorCommandContext context) {
        this.context = context;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length < 2) {
            context.send(player, "&eUsage: /ite create <name>");
            return true;
        }
        String name = args[1];
        if (context.findTrackByName(name).isPresent()) {
            context.send(player, "&cTrack already exists.");
            return true;
        }
        try {
            Track track = context.trackService().createTrack(name, player.getWorld().getName());
            context.send(player, "&aCreated track &f" + track.getName() + "&a in world &f" + track.getWorld());
        } catch (SQLException exception) {
            context.send(player, "&cDatabase error: " + exception.getMessage());
        }
        return true;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "/ite create <name>";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.EDITOR_MODIFY);
    }
}

