package com.hegi64.iceBoatTimeTrial.command.sub.editor;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ForceStopEditorSubCommand implements SubCommand {
    private final EditorCommandContext context;

    public ForceStopEditorSubCommand(EditorCommandContext context) {
        this.context = context;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            context.send(sender, "&eUsage: /ite forcestop <trackname>");
            return true;
        }

        Optional<Track> trackOpt = context.findTrackByName(args[1]);
        if (trackOpt.isEmpty()) {
            context.send(sender, "&cTrack not found.");
            return true;
        }

        Track track = trackOpt.get();
        Optional<UUID> stopped = context.sessions().forceStopTrack(track.getUuid());
        if (stopped.isEmpty()) {
            context.send(sender, "&eNo active editor for this track.");
            return true;
        }

        context.send(sender, "&aForce-stopped editor session for &f" + track.getName());
        return true;
    }

    @Override
    public String getName() {
        return "forcestop";
    }

    @Override
    public String getDescription() {
        return "/ite forcestop <trackname>";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.EDITOR_ADMIN);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            return context.partial(args[1], context.trackService().allTracks().stream().map(Track::getName).toList());
        }
        return List.of();
    }
}

