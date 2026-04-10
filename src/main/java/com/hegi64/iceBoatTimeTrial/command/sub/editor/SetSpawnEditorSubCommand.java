package com.hegi64.iceBoatTimeTrial.command.sub.editor;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.model.RegionBox;
import com.hegi64.iceBoatTimeTrial.model.RegionType;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class SetSpawnEditorSubCommand implements SubCommand {
    private final EditorCommandContext context;
    public SetSpawnEditorSubCommand(EditorCommandContext context) {
        this.context = context;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.send(sender, "&cOnly players can use this command.");
            return true;
        }

        Track track;
        try {
            track = context.requireEditedTrack(player);
        } catch (IllegalStateException exception) {
            context.send(player, "&c" + exception.getMessage());
            return true;
        }

        if (!track.getWorld().equals(player.getWorld().getName())) {
            context.send(player, "&cTrack world mismatch. Track world is &f" + track.getWorld());
            return true;
        }

        try {
            Location location = player.getLocation();
            context.trackService().setSpawn(track, location);
        } catch (IllegalStateException exception) {
            context.send(player, "&c" + exception.getMessage());
            return true;
        } catch (SQLException exception) {
            context.send(player, "&cDatabase error: " + exception.getMessage());
            return true;
        }

        context.send(player, "&aUpdated Spawn Location for &f" + track.getName());
        return true;
    }

    @Override
    public String getName() {
        return "setspawn";
    }

    @Override
    public String getDescription() {
        return "/ite setspawn";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.EDITOR_MODIFY);
    }
}

