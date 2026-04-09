package com.hegi64.iceBoatTimeTrial.hologram;

import com.hegi64.iceBoatTimeTrial.config.PluginConfig;
import com.hegi64.iceBoatTimeTrial.model.Track;
import com.hegi64.iceBoatTimeTrial.service.TrackService;
import com.hegi64.iceBoatTimeTrial.storage.Database;
import com.hegi64.iceBoatTimeTrial.util.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HologramUpdater {
    private final HologramService hologramService;
    private final TrackService trackService;
    private final Database database;
    private PluginConfig config;

    private static final List<String> SUPPORTED_TYPES = List.of(
            "top_times", "top_players",
            "top_times_s1", "top_times_s2", "top_times_s3",
            "top_players_s1", "top_players_s2", "top_players_s3"
    );

    public static List<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    public HologramUpdater(HologramService hologramService, TrackService trackService, Database database, PluginConfig config) {
        this.hologramService = hologramService;
        this.trackService = trackService;
        this.database = database;
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

        int limit = config.hologramLimit();
        LeaderboardType leaderboardType = LeaderboardType.from(type);
        String titleTemplate = titleForType(leaderboardType);
        String empty = config.hologramEmptyLine();

        List<String> lines = new ArrayList<>();
        lines.add(titleTemplate
                .replace("%track%", trackName)
                .replace("%sector%", Integer.toString(leaderboardType.sector())));

        Optional<Track> trackOpt = trackService.findByName(trackName);
        if (trackOpt.isEmpty()) {
            lines.add(empty);
            hologramService.updateHologram(trackName, type, lines);
            return;
        }

        List<String> entries = leaderboardType.playerBest()
                ? getTopPlayers(trackOpt.get().getUuid(), limit, leaderboardType.sector())
                : getTopTimes(trackOpt.get().getUuid(), limit, leaderboardType.sector());

        if (entries.isEmpty()) {
            lines.add(empty);
        } else {
            lines.addAll(entries);
        }
        hologramService.updateHologram(trackName, type, lines);
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
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
                result.add(config.hologramEntryTemplate()
                        .replace("%rank%", Integer.toString(rank))
                        .replace("%player%", playerName)
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
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
                result.add(config.hologramEntryPlayersTemplate()
                        .replace("%rank%", Integer.toString(rank))
                        .replace("%player%", playerName)
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
