package com.hegi64.iceBoatTimeTrial.hologram;

import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.service.analytics.LeaderboardAnalyticsService;
import com.hegi64.iceBoatTimeTrial.service.analytics.LeaderboardEntry;
import com.hegi64.iceBoatTimeTrial.service.analytics.PeriodFilter;
import com.hegi64.iceBoatTimeTrial.storage.Database;
import com.hegi64.iceBoatTimeTrial.util.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public class HologramUpdater {
    private final HologramService hologramService;
    private final TrackService trackService;
    private final Database database;
    private final LeaderboardAnalyticsService analytics;
    private PluginConfig config;

    private static final List<String> SUPPORTED_TYPES = List.of(
            "top_times", "top_players",
            "top_times_s1", "top_times_s2", "top_times_s3",
            "top_players_s1", "top_players_s2", "top_players_s3",
            "recent_pbs", "track_info", "top_improved", "top_consistent"
    );

    public static List<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    public HologramUpdater(HologramService hologramService,
                           TrackService trackService,
                           Database database,
                           LeaderboardAnalyticsService analytics,
                           PluginConfig config) {
        this.hologramService = hologramService;
        this.trackService = trackService;
        this.database = database;
        this.analytics = analytics;
        this.config = config;
    }

    public void setConfig(PluginConfig config) {
        this.config = config;
    }

    public void updateAll() {
        for (HologramService.HologramMeta meta : hologramService.getPlacedHolograms()) {
            update(meta.track(), meta.type());
        }
    }

    public void update(String trackName, String type) {
        if (!SUPPORTED_TYPES.contains(type)) {
            return;
        }

        Optional<Track> trackOpt = trackService.findByName(trackName);
        List<String> lines = new ArrayList<>();
        if (trackOpt.isEmpty()) {
            lines.add("&cTrack not found: " + trackName);
            hologramService.updateHologram(trackName, type, lines);
            return;
        }
        Track track = trackOpt.get();

        if (type.equals("recent_pbs")) {
            lines.add(config.hologramRecentPbsTitle().replace("%track%", trackName));
            lines.addAll(buildRecentPbs(track));
        } else if (type.equals("track_info")) {
            lines.add(config.hologramTrackInfoTitle().replace("%track%", trackName));
            lines.addAll(buildTrackInfo(track));
        } else if (type.equals("top_improved")) {
            lines.add(config.hologramTopImprovedTitle().replace("%track%", trackName));
            lines.addAll(buildImproved(track));
        } else if (type.equals("top_consistent")) {
            lines.add(config.hologramTopConsistentTitle().replace("%track%", trackName));
            lines.addAll(buildConsistent(track));
        } else {
            int limit = config.hologramLimit();
            LeaderboardType leaderboardType = LeaderboardType.from(type);
            String titleTemplate = titleForType(leaderboardType);
            lines.add(titleTemplate
                    .replace("%track%", trackName)
                    .replace("%sector%", Integer.toString(leaderboardType.sector())));
            List<String> entries = leaderboardType.playerBest()
                    ? getTopPlayers(track.getUuid(), limit, leaderboardType.sector())
                    : getTopTimes(track.getUuid(), limit, leaderboardType.sector());
            if (entries.isEmpty()) {
                lines.add(config.hologramEmptyLine());
            } else {
                lines.addAll(entries);
            }
        }

        if (lines.size() == 1) {
            lines.add(config.hologramEmptyLine());
        }
        hologramService.updateHologram(trackName, type, lines);
    }

    private List<String> buildRecentPbs(Track track) {
        try {
            List<Database.RecentPbRow> rows = database.getRecentPbEvents(track.getUuid(), config.hologramLimit());
            if (rows.isEmpty()) {
                return List.of(config.hologramEmptyLine());
            }
            List<String> lines = new ArrayList<>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
            int rank = 1;
            for (Database.RecentPbRow row : rows) {
                String player = resolveName(row.playerUuid());
                lines.add(config.hologramRecentPbEntry()
                        .replace("%rank%", Integer.toString(rank))
                        .replace("%player%", player)
                        .replace("%time%", TimeFormat.formatMillis(row.totalMs()))
                        .replace("%date%", fmt.format(Instant.ofEpochMilli(row.finishedAt()))));
                rank++;
            }
            return lines;
        } catch (SQLException exception) {
            return List.of(config.hologramEmptyLine());
        }
    }

    private List<String> buildTrackInfo(Track track) {
        try {
            long runs = database.getTrackRunCount(track.getUuid());
            OptionalLong last = database.getTrackLastActivity(track.getUuid());
            String lastActivity = last.isPresent()
                    ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(last.getAsLong()))
                    : "n/a";
            return List.of(
                    config.hologramTrackInfoLineWorld().replace("%world%", track.getWorld()),
                    config.hologramTrackInfoLineEnabled().replace("%enabled%", Boolean.toString(track.isEnabled())),
                    config.hologramTrackInfoLineRuns().replace("%runs%", Long.toString(runs)),
                    config.hologramTrackInfoLineLastActivity().replace("%last_activity%", lastActivity)
            );
        } catch (SQLException exception) {
            return List.of(config.hologramEmptyLine());
        }
    }

    private List<String> buildImproved(Track track) {
        List<LeaderboardEntry> rows = analytics.getMostImproved(track.getUuid(), PeriodFilter.ALL, config.hologramLimit());
        if (rows.isEmpty()) {
            return List.of(config.hologramEmptyLine());
        }
        List<String> lines = new ArrayList<>();
        int rank = 1;
        for (LeaderboardEntry row : rows) {
            lines.add(config.hologramImprovedEntry()
                    .replace("%rank%", Integer.toString(rank))
                    .replace("%player%", resolveName(row.playerUuid()))
                    .replace("%value%", TimeFormat.formatMillis(row.value())));
            rank++;
        }
        return lines;
    }

    private List<String> buildConsistent(Track track) {
        int sampleSize = config.leaderboardConsistencySampleSize();
        List<LeaderboardEntry> rows = analytics.getMostConsistent(track.getUuid(), PeriodFilter.ALL, config.hologramLimit(), sampleSize);
        if (rows.isEmpty()) {
            return List.of(config.hologramEmptyLine());
        }
        List<String> lines = new ArrayList<>();
        int rank = 1;
        for (LeaderboardEntry row : rows) {
            lines.add(config.hologramConsistentEntry()
                    .replace("%rank%", Integer.toString(rank))
                    .replace("%player%", resolveName(row.playerUuid()))
                    .replace("%value%", TimeFormat.formatMillis(row.value())));
            rank++;
        }
        return lines;
    }

    private String resolveName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
    }

    private String titleForType(LeaderboardType type) {
        if (!type.playerBest() && type.sector() == 0) {
            return config.hologramTopTimesTitle();
        }
        if (type.playerBest() && type.sector() == 0) {
            return config.hologramTopPlayersTitle();
        }
        if (!type.playerBest()) {
            return config.hologramTopTimesSectorTitle();
        }
        return config.hologramTopPlayersSectorTitle();
    }

    private List<String> getTopTimes(UUID trackUuid, int limit, int sector) {
        try {
            List<String> db = database.getTopTimesFromHistory(trackUuid, limit, sector);
            List<String> result = new ArrayList<>();
            int rank = 1;
            for (String line : db) {
                String[] parts = line.split(":", 2);
                UUID uuid = UUID.fromString(parts[0]);
                long time = Long.parseLong(parts[1]);
                result.add(config.hologramEntryTemplate()
                        .replace("%rank%", Integer.toString(rank))
                        .replace("%player%", resolveName(uuid))
                        .replace("%time%", TimeFormat.formatMillis(time)));
                rank++;
            }
            return result;
        } catch (SQLException exception) {
            return List.of();
        }
    }

    private List<String> getTopPlayers(UUID trackUuid, int limit, int sector) {
        try {
            List<String> db = database.getTopPlayers(trackUuid, limit, sector);
            List<String> result = new ArrayList<>();
            int rank = 1;
            for (String line : db) {
                String[] parts = line.split(":", 2);
                UUID uuid = UUID.fromString(parts[0]);
                long time = Long.parseLong(parts[1]);
                result.add(config.hologramEntryPlayersTemplate()
                        .replace("%rank%", Integer.toString(rank))
                        .replace("%player%", resolveName(uuid))
                        .replace("%time%", TimeFormat.formatMillis(time)));
                rank++;
            }
            return result;
        } catch (SQLException exception) {
            return List.of();
        }
    }

    private record LeaderboardType(boolean playerBest, int sector) {
        static LeaderboardType from(String type) {
            return switch (type) {
                case "top_players" -> new LeaderboardType(true, 0);
                case "top_players_s1" -> new LeaderboardType(true, 1);
                case "top_players_s2" -> new LeaderboardType(true, 2);
                case "top_players_s3" -> new LeaderboardType(true, 3);
                case "top_times_s1" -> new LeaderboardType(false, 1);
                case "top_times_s2" -> new LeaderboardType(false, 2);
                case "top_times_s3" -> new LeaderboardType(false, 3);
                default -> new LeaderboardType(false, 0);
            };
        }
    }
}
