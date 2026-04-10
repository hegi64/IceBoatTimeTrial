package com.hegi64.iceBoatTimeTrial.command.sub.editor;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class ForceEditEditorSubCommand implements SubCommand {
    private final EditorCommandContext context;

    public ForceEditEditorSubCommand(EditorCommandContext context) {
        this.context = context;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.send(sender, "&cOnly players can use this command.");
            return true;
        }
        if (args.length < 2) {
            context.send(player, "&eUsage: /ite forceedit <trackname>");
            return true;
        }
        Optional<Track> trackOpt = context.findTrackByName(args[1]);
        if (trackOpt.isEmpty()) {
            context.send(player, "&cTrack not found.");
            return true;
        }
        Track track = trackOpt.get();

        context.sessions().forceStopTrack(track.getUuid());
        context.sessions().stopSession(player.getUniqueId());
        context.sessions().startSession(player.getUniqueId(), track);
        context.send(player, "&aForce-enabled editor mode for &f" + track.getName());
        return context.summary(player);
    }

    @Override
    public String getName() {
        return "forceedit";
    }

    @Override
    public String getDescription() {
        return "/ite forceedit <trackname>";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.EDITOR_ADMIN);
    }

    @Override
    public java.util.List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            return context.partial(args[1], context.trackService().allTracks().stream().map(Track::getName).toList());
        }
        return java.util.List.of();
    }
}

