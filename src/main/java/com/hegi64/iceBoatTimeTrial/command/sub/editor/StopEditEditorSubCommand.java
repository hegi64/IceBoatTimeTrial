package com.hegi64.iceBoatTimeTrial.command.sub.editor;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.editor.TrackEditorSession;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class StopEditEditorSubCommand implements SubCommand {
    private final EditorCommandContext context;

    public StopEditEditorSubCommand(EditorCommandContext context) {
        this.context = context;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.send(sender, "&cOnly players can use this command.");
            return true;
        }

        Optional<TrackEditorSession> sessionOpt = context.sessions().getByPlayer(player.getUniqueId());
        if (sessionOpt.isEmpty()) {
            context.send(player, "&cYou are not in editor mode.");
            return true;
        }

        TrackEditorSession session = sessionOpt.get();
        if (args.length >= 2) {
            Optional<Track> byName = context.findTrackByName(args[1]);
            if (byName.isEmpty() || !byName.get().getUuid().equals(session.trackUuid())) {
                context.send(player, "&cYou are not editing that track.");
                return true;
            }
        }

        context.sessions().stopSession(player.getUniqueId());
        context.send(player, "&eEditor mode disabled.");
        return true;
    }

    @Override
    public String getName() {
        return "stopedit";
    }

    @Override
    public String getDescription() {
        return "/ite stopedit [trackname]";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.EDITOR_USE);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            return context.partial(args[1], context.trackService().allTracks().stream().map(Track::getName).toList());
        }
        return List.of();
    }
}

