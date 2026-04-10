package com.hegi64.iceBoatTimeTrial.command.sub.editor;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.model.RegionType;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class SetDirectionEditorSubCommand implements SubCommand {
    private final EditorCommandContext context;

    public SetDirectionEditorSubCommand(EditorCommandContext context) {
        this.context = context;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length < 5) {
            context.send(player, "&eUsage: /ite setdir <start|cp1|cp2> <x> <y> <z> [threshold]");
            return true;
        }

        Track track;
        try {
            track = context.requireEditedTrack(player);
        } catch (IllegalStateException exception) {
            context.send(player, "&c" + exception.getMessage());
            return true;
        }

        RegionType type = context.parseRegionType(args[1]);
        if (type == null) {
            context.send(player, "&cUnknown region type. Use start|cp1|cp2");
            return true;
        }

        double x;
        double y;
        double z;
        try {
            x = Double.parseDouble(args[2]);
            y = Double.parseDouble(args[3]);
            z = Double.parseDouble(args[4]);
        } catch (NumberFormatException exception) {
            context.send(player, "&cDirection must be numeric.");
            return true;
        }

        Double threshold = null;
        if (args.length >= 6) {
            try {
                threshold = Double.parseDouble(args[5]);
            } catch (NumberFormatException exception) {
                context.send(player, "&cThreshold must be numeric.");
                return true;
            }
        }

        try {
            context.trackService().setDirection(track, type, x, y, z, threshold);
        } catch (SQLException exception) {
            context.send(player, "&cDatabase error: " + exception.getMessage());
            return true;
        }

        context.send(player, "&aDirection updated for &f" + type.name());
        return context.summary(player);
    }

    @Override
    public String getName() {
        return "setdir";
    }

    @Override
    public String getDescription() {
        return "/ite setdir <start|cp1|cp2> <x> <y> <z> [threshold]";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.EDITOR_MODIFY);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            return context.partial(args[1], Arrays.asList("start", "cp1", "cp2"));
        }
        return List.of();
    }
}

