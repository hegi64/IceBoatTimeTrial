package com.hegi64.iceBoatTimeTrial.service.analytics;

import com.hegi64.iceBoatTimeTrial.storage.Database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SqliteLeaderboardAnalyticsService implements LeaderboardAnalyticsService {
    private final Database database;

    public SqliteLeaderboardAnalyticsService(Database database) {
        this.database = database;
    }

    @Override
    public List<LeaderboardEntry> getMostImproved(UUID trackUuid, PeriodFilter period, int limit) {
        try {
            long minFinishedAt = minFinishedAt(period);
            List<Database.PlayerRunSample> rows = database.getPlayerRunSamples(trackUuid, minFinishedAt);
            Map<UUID, long[]> firstAndBest = new HashMap<>();
            for (Database.PlayerRunSample row : rows) {
                long[] values = firstAndBest.computeIfAbsent(row.playerUuid(), ignored -> new long[]{row.totalMs(), row.totalMs()});
                values[1] = Math.min(values[1], row.totalMs());
            }

            List<LeaderboardEntry> result = new ArrayList<>();
            for (Map.Entry<UUID, long[]> entry : firstAndBest.entrySet()) {
                long first = entry.getValue()[0];
                long best = entry.getValue()[1];
                long improvement = Math.max(0L, first - best);
                if (improvement > 0L) {
                    result.add(new LeaderboardEntry(entry.getKey(), null, improvement, ""));
                }
            }
            result.sort(Comparator.comparingLong(LeaderboardEntry::value).reversed());
            return result.subList(0, Math.min(limit, result.size()));
        } catch (SQLException exception) {
            return List.of();
        }
    }

    @Override
    public List<LeaderboardEntry> getMostConsistent(UUID trackUuid, PeriodFilter period, int limit, int sampleSize) {
        try {
            long minFinishedAt = minFinishedAt(period);
            List<Database.PlayerRunSample> rows = database.getPlayerRunSamples(trackUuid, minFinishedAt);
            Map<UUID, List<Long>> byPlayer = new HashMap<>();
            for (Database.PlayerRunSample row : rows) {
                byPlayer.computeIfAbsent(row.playerUuid(), ignored -> new ArrayList<>()).add(row.totalMs());
            }

            int requiredSamples = Math.max(3, sampleSize);
            List<LeaderboardEntry> result = new ArrayList<>();
            for (Map.Entry<UUID, List<Long>> entry : byPlayer.entrySet()) {
                List<Long> values = entry.getValue();
                if (values.size() < requiredSamples) {
                    continue;
                }
                List<Long> tail = values.subList(values.size() - requiredSamples, values.size());
                long stdev = Math.round(standardDeviation(tail));
                result.add(new LeaderboardEntry(entry.getKey(), null, stdev, "n=" + requiredSamples));
            }
            result.sort(Comparator.comparingLong(LeaderboardEntry::value));
            return result.subList(0, Math.min(limit, result.size()));
        } catch (SQLException exception) {
            return List.of();
        }
    }

    @Override
    public List<TrackActivityEntry> getMostActiveTracks(PeriodFilter period, int limit) {
        try {
            long minFinishedAt = minFinishedAt(period);
            List<Database.TrackActivityRow> rows = database.getTrackActivity(minFinishedAt, limit);
            List<TrackActivityEntry> result = new ArrayList<>();
            for (Database.TrackActivityRow row : rows) {
                result.add(new TrackActivityEntry(row.trackUuid(), row.trackName(), row.runCount()));
            }
            return result;
        } catch (SQLException exception) {
            return List.of();
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

    private double standardDeviation(List<Long> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double mean = values.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> {
                    double d = v - mean;
                    return d * d;
                })
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
}

