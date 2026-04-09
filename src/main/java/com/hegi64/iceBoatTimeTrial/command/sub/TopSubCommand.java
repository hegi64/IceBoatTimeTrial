package com.hegi64.iceBoatTimeTrial.command.sub;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.storage.Database;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import com.hegi64.iceBoatTimeTrial.util.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class TopSubCommand implements SubCommand {
    private final PluginConfig config;
    private final TrackService trackService;
    private final Database database;

    public TopSubCommand(PluginConfig config, TrackService trackService, Database database) {
        this.config = config;
        this.trackService = trackService;
        this.database = database;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            send(sender, "&eUsage: /ibt top <track> [limit] [all]");
            return true;
        }
        Optional<Track> trackOpt = trackService.findByName(args[1]);
        if (trackOpt.isEmpty()) {
            send(sender, "&cTrack not found.");
            return true;
        }

        int limit = config.leaderboardDefaultLimit();
        if (args.length >= 3) {
            try {
                limit = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException exception) {
                send(sender, "&cLimit must be a number.");
                return true;
            }
        }

        boolean showAll = args.length >= 4 && args[3].equalsIgnoreCase("all");
        try {
            List<String> top = showAll
                    ? database.getTopTimesFromHistory(trackOpt.get().getUuid(), limit)
                    : database.getTopTimes(trackOpt.get().getUuid(), limit);
            if (top.isEmpty()) {
                send(sender, "&eNo entries for this track yet.");
                return true;
            }
            send(sender, "&bTop " + top.size() + " on " + trackOpt.get().getName() + (showAll ? " (all times):" : ":"));
            int rank = 1;
            for (String line : top) {
                String[] parts = line.split(":", 2);
                UUID uuid = UUID.fromString(parts[0]);
                long time = Long.parseLong(parts[1]);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
                send(sender, "&7#" + rank + " &f" + playerName + " &7- &e" + TimeFormat.formatMillis(time));
                rank++;
            }
        } catch (SQLException exception) {
            send(sender, "&cDatabase error: " + exception.getMessage());
        }
        return true;
    }

    @Override
    public String getName() {
        return "top";
    }

    @Override
    public String getDescription() {
        return "/ibt top <track> [limit] [all]";
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
        if (args.length == 4) {
            return partial(args[3], List.of("all"));
        }
        return List.of();
    }

    private List<String> partial(String token, List<String> candidates) {
        String lower = token.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Chat.color(config.messagePrefix() + message));
    }
}
