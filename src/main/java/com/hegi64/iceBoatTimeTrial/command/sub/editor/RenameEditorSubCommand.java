package com.hegi64.iceBoatTimeTrial.command.sub.editor;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class RenameEditorSubCommand implements SubCommand {
    private final EditorCommandContext context;

    public RenameEditorSubCommand(EditorCommandContext context) {
        this.context = context;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length < 2) {
            context.send(player, "&eUsage: /ite rename <newname>");
            return true;
        }

        Track track;
        try {
            track = context.requireEditedTrack(player);
        } catch (IllegalStateException exception) {
            context.send(player, "&c" + exception.getMessage());
            return true;
        }

        String old = track.getName();
        String next = args[1];
        try {
            if (!context.trackService().renameTrack(track, next)) {
                context.send(player, "&cCould not rename. Name may already exist.");
                return true;
            }
        } catch (SQLException exception) {
            context.send(player, "&cDatabase error: " + exception.getMessage());
            return true;
        }

        context.send(player, "&aRenamed track: &f" + old + " &7-> &f" + next);
        return context.summary(player);
    }

    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getDescription() {
        return "/ite rename <newname>";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.EDITOR_MODIFY);
    }
}

