package com.hegi64.iceBoatTimeTrial.command.sub;

import com.hegi64.iceBoatTimeTrial.command.SubCommand;
import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.security.Permissions;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.service.analytics.LeaderboardAnalyticsService;
import com.hegi64.iceBoatTimeTrial.service.analytics.LeaderboardEntry;
import com.hegi64.iceBoatTimeTrial.service.analytics.PeriodFilter;
import com.hegi64.iceBoatTimeTrial.service.analytics.TrackActivityEntry;
import com.hegi64.iceBoatTimeTrial.storage.Database;
import com.hegi64.iceBoatTimeTrial.util.Chat;
import com.hegi64.iceBoatTimeTrial.util.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class TopSubCommand implements SubCommand {
    private final PluginConfig config;
    private final TrackService trackService;
    private final Database database;
    private final LeaderboardAnalyticsService analytics;

    public TopSubCommand(PluginConfig config, TrackService trackService, Database database, LeaderboardAnalyticsService analytics) {
        this.config = config;
        this.trackService = trackService;
        this.database = database;
        this.analytics = analytics;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        // New only: /ibt top <mode> [track] [period] [limit]
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        Mode mode = parseMode(args[1]);
        if (mode == null) {
            sendUsage(sender);
            return true;
        }

        return switch (mode) {
            case TIMES, PLAYERS, IMPROVED, CONSISTENT -> handleTrackMode(sender, args, mode);
            case ACTIVE -> handleActiveMode(sender, args);
        };
    }

    private boolean handleTrackMode(CommandSender sender, String[] args, Mode mode) {
        if (args.length < 3) {
            send(sender, "&eUsage: /ibt top " + mode.name().toLowerCase(Locale.ROOT) + " <track> [day|week|month|all] [limit]");
            return true;
        }

        Optional<Track> trackOpt = trackService.findByName(args[2]);
        if (trackOpt.isEmpty()) {
            send(sender, "&cTrack not found.");
            return true;
        }

        PeriodFilter period = parsePeriod(args, 3);
        int limit = parseLimit(args, 4);
        if (limit < 1) {
            send(sender, "&cLimit must be a number.");
            return true;
        }

        Track track = trackOpt.get();
        return switch (mode) {
            case TIMES -> handleTimes(sender, track, period, limit);
            case PLAYERS -> handlePlayers(sender, track, period, limit);
            case IMPROVED -> handleImproved(sender, track, period, limit);
            case CONSISTENT -> handleConsistent(sender, track, period, limit);
            default -> true;
        };
    }

    private boolean handleTimes(CommandSender sender, Track track, PeriodFilter period, int limit) {
        try {
            long from = minFinishedAt(period);
            List<String> lines = database.getTopTimesFromHistory(track.getUuid(), limit, 0, from);
            if (lines.isEmpty()) {
                send(sender, "&eNo entries for this selection.");
                return true;
            }
            send(sender, "&bTop times on &f" + track.getName() + " &7(" + period.name().toLowerCase(Locale.ROOT) + "):");
            printTimeLines(sender, lines);
        } catch (SQLException exception) {
            send(sender, "&cDatabase error: " + exception.getMessage());
        }
        return true;
    }

    private boolean handlePlayers(CommandSender sender, Track track, PeriodFilter period, int limit) {
        try {
            long from = minFinishedAt(period);
            List<String> lines = database.getTopPlayersFromHistory(track.getUuid(), limit, 0, from);
            if (lines.isEmpty()) {
                send(sender, "&eNo entries for this selection.");
                return true;
            }
            send(sender, "&bTop players on &f" + track.getName() + " &7(" + period.name().toLowerCase(Locale.ROOT) + "):");
            printTimeLines(sender, lines);
        } catch (SQLException exception) {
            send(sender, "&cDatabase error: " + exception.getMessage());
        }
        return true;
    }

    private boolean handleImproved(CommandSender sender, Track track, PeriodFilter period, int limit) {
        List<LeaderboardEntry> rows = analytics.getMostImproved(track.getUuid(), period, limit);
        if (rows.isEmpty()) {
            send(sender, "&eNo improvement entries for this selection.");
            return true;
        }
        send(sender, "&bMost improved on &f" + track.getName() + " &7(" + period.name().toLowerCase(Locale.ROOT) + "):");
        int rank = 1;
        for (LeaderboardEntry row : rows) {
            send(sender, "&7#" + rank + " &f" + resolveName(row.playerUuid()) + " &7- &a" + TimeFormat.formatMillis(row.value()));
            rank++;
        }
        return true;
    }

    private boolean handleConsistent(CommandSender sender, Track track, PeriodFilter period, int limit) {
        int sampleSize = config.leaderboardConsistencySampleSize();
        List<LeaderboardEntry> rows = analytics.getMostConsistent(track.getUuid(), period, limit, sampleSize);
        if (rows.isEmpty()) {
            send(sender, "&eNo consistency entries for this selection.");
            return true;
        }
        send(sender, "&bMost consistent on &f" + track.getName() + " &7(" + period.name().toLowerCase(Locale.ROOT) + ", n=" + sampleSize + "):");
        int rank = 1;
        for (LeaderboardEntry row : rows) {
            send(sender, "&7#" + rank + " &f" + resolveName(row.playerUuid()) + " &7- &e" + TimeFormat.formatMillis(row.value()));
            rank++;
        }
        return true;
    }

    private boolean handleActiveMode(CommandSender sender, String[] args) {
        PeriodFilter period = parsePeriod(args, 2);
        int limit = parseLimit(args, 3);
        if (limit < 1) {
            send(sender, "&cLimit must be a number.");
            return true;
        }

        List<TrackActivityEntry> rows = analytics.getMostActiveTracks(period, limit);
        if (rows.isEmpty()) {
            send(sender, "&eNo active-track entries for this selection.");
            return true;
        }
        send(sender, "&bMost active tracks &7(" + period.name().toLowerCase(Locale.ROOT) + "):");
        int rank = 1;
        for (TrackActivityEntry row : rows) {
            send(sender, "&7#" + rank + " &f" + row.trackName() + " &7- &e" + row.runCount() + " runs");
            rank++;
        }
        return true;
    }

    private void printTimeLines(CommandSender sender, List<String> lines) {
        int rank = 1;
        for (String line : lines) {
            String[] parts = line.split(":", 2);
            UUID uuid = UUID.fromString(parts[0]);
            long time = Long.parseLong(parts[1]);
            send(sender, "&7#" + rank + " &f" + resolveName(uuid) + " &7- &e" + TimeFormat.formatMillis(time));
            rank++;
        }
    }

    private String resolveName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
    }

    private Mode parseMode(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "times", "top_times" -> Mode.TIMES;
            case "players", "top_players" -> Mode.PLAYERS;
            case "improved", "most_improved" -> Mode.IMPROVED;
            case "consistent", "most_consistent" -> Mode.CONSISTENT;
            case "active", "most_active_tracks" -> Mode.ACTIVE;
            default -> null;
        };
    }

    private PeriodFilter parsePeriod(String[] args, int index) {
        if (args.length <= index) {
            return PeriodFilter.ALL;
        }
        return switch (args[index].toLowerCase(Locale.ROOT)) {
            case "day", "d" -> PeriodFilter.DAY;
            case "week", "w" -> PeriodFilter.WEEK;
            case "month", "m" -> PeriodFilter.MONTH;
            case "all", "a" -> PeriodFilter.ALL;
            default -> PeriodFilter.ALL;
        };
    }

    private int parseLimit(String[] args, int index) {
        int defaultLimit = config.leaderboardDefaultLimit();
        if (args.length <= index) {
            return defaultLimit;
        }
        try {
            return Math.max(1, Integer.parseInt(args[index]));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private long minFinishedAt(PeriodFilter period) {
        long now = System.currentTimeMillis();
        return switch (period) {
            case DAY -> now - 24L * 60L * 60L * 1000L;
            case WEEK -> now - 7L * 24L * 60L * 60L * 1000L;
            case MONTH -> now - 30L * 24L * 60L * 60L * 1000L;
            case ALL -> 0L;
        };
    }

    private void sendUsage(CommandSender sender) {
        send(sender, "&eUsage: /ibt top <times|players|improved|consistent> <track> [day|week|month|all] [limit]");
        send(sender, "&eUsage: /ibt top active [day|week|month|all] [limit]");
    }

    @Override
    public String getName() {
        return "top";
    }

    @Override
    public String getDescription() {
        return "/ibt top <mode> ...";
    }

    @Override
    public boolean hasRequiredPermission(CommandSender sender) {
        return sender.hasPermission(Permissions.PLAYER);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            return partial(args[1], List.of("times", "players", "improved", "consistent", "active"));
        }
        if (args.length == 3) {
            Mode mode = parseMode(args[1]);
            if (mode == Mode.ACTIVE) {
                return partial(args[2], List.of("day", "week", "month", "all"));
            }
            if (mode != null) {
                return partial(args[2], trackService.allTracks().stream().map(Track::getName).toList());
            }
        }
        if (args.length == 4) {
            Mode mode = parseMode(args[1]);
            if (mode == Mode.ACTIVE) {
                return partial(args[3], List.of("10", "25", "50"));
            }
            if (mode != null) {
                return partial(args[3], List.of("day", "week", "month", "all"));
            }
        }
        if (args.length == 5) {
            Mode mode = parseMode(args[1]);
            if (mode != null && mode != Mode.ACTIVE) {
                return partial(args[4], List.of("10", "25", "50"));
            }
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

    private enum Mode {
        TIMES,
        PLAYERS,
        IMPROVED,
        CONSISTENT,
        ACTIVE
    }
}
