package com.hegi64.iceBoatTimeTrial.command.sub;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.storage.Database;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import com.hegi64.iceBoatTimeTrial.util.TimeFormat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class BestSubCommand implements SubCommand {
    private final PluginConfig config;
    private final TrackService trackService;
    private final Database database;

    public BestSubCommand(PluginConfig config, TrackService trackService, Database database) {
        this.config = config;
        this.trackService = trackService;
        this.database = database;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly players can query personal bests.");
            return true;
        }

        if (args.length < 2) {
            return handleBestAllTracks(player);
        }

        Optional<Track> trackOpt = trackService.findByName(args[1]);
        if (trackOpt.isEmpty()) {
            send(sender, "&cTrack not found.");
            return true;
        }
        return handleBestSingleTrack(player, trackOpt.get());
    }

    @Override
    public String getName() {
        return "best";
    }

    @Override
    public String getDescription() {
        return "/ibt best [track]";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.PLAYER);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            return partial(args[1], trackService.allTracks().stream().map(Track::getName).toList());
        }
        return List.of();
    }

    private boolean handleBestAllTracks(Player player) {
        boolean found = false;
        for (Track track : trackService.allTracks()) {
            try {
                Optional<long[]> best = database.getBest(player.getUniqueId(), track.getUuid());
                if (best.isEmpty()) {
                    continue;
                }
                found = true;
                long[] values = best.get();
                send(player, "&bPB " + track.getName() + ": &f" + TimeFormat.formatMillis(values[0])
                        + " &7(S1 " + TimeFormat.formatMillis(values[1])
                        + " | S2 " + TimeFormat.formatMillis(values[2])
                        + " | S3 " + TimeFormat.formatMillis(values[3]) + ")");
            } catch (SQLException exception) {
                send(player, "&cDatabase error: " + exception.getMessage());
                return true;
            }
        }
        if (!found) {
            send(player, "&eNo best time recorded yet.");
        }
        return true;
    }

    private boolean handleBestSingleTrack(Player player, Track track) {
        try {
            Optional<long[]> best = database.getBest(player.getUniqueId(), track.getUuid());
            if (best.isEmpty()) {
                send(player, "&eNo best time recorded yet.");
                return true;
            }
            long[] values = best.get();
            send(player, "&bPB " + track.getName() + ": &f" + TimeFormat.formatMillis(values[0]));
            send(player, "&7S1: &f" + TimeFormat.formatMillis(values[1])
                    + " &7S2: &f" + TimeFormat.formatMillis(values[2])
                    + " &7S3: &f" + TimeFormat.formatMillis(values[3]));
        } catch (SQLException exception) {
            send(player, "&cDatabase error: " + exception.getMessage());
        }
        return true;
    }

    private List<String> partial(String token, List<String> candidates) {
        String lower = token.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }
}
