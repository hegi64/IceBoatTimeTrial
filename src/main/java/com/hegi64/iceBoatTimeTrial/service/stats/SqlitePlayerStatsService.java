package com.hegi64.iceBoatTimeTrial.service.stats;

import com.hegi64.iceBoatTimeTrial.service.analytics.PeriodFilter;
import com.hegi64.iceBoatTimeTrial.storage.Database;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.UUID;

public class SqlitePlayerStatsService implements PlayerStatsService {
    private final Database database;

    public SqlitePlayerStatsService(Database database) {
        this.database = database;
    }

    @Override
    public Optional<PlayerStatsSnapshot> getStats(UUID playerUuid, PeriodFilter period) {
        long minFinishedAt = minFinishedAt(period);
        try {
            long totalRuns = database.countPlayerRuns(playerUuid, minFinishedAt, false);
            long totalCompletions = database.countPlayerRuns(playerUuid, minFinishedAt, true);
            if (totalRuns <= 0L) {
                return Optional.empty();
            }

            OptionalLong bestOverall = database.getPlayerBestTotal(playerUuid, minFinishedAt);
            OptionalDouble recentAverage = database.getPlayerRecentAverage(playerUuid, minFinishedAt, 10);
            long improvement = calculateImprovement(playerUuid, minFinishedAt);

            return Optional.of(new PlayerStatsSnapshot(
                    playerUuid,
                    null,
                    totalRuns,
                    totalCompletions,
                    bestOverall.orElse(-1L),
                    recentAverage.isPresent() ? Math.round(recentAverage.getAsDouble()) : -1L,
                    improvement
            ));
        } catch (SQLException exception) {
            return Optional.empty();
        }
    }

    private long calculateImprovement(UUID playerUuid, long minFinishedAt) throws SQLException {
        List<Database.PlayerRunSample> rows = database.getPlayerRunSamplesForPlayer(playerUuid, minFinishedAt);
        if (rows.size() < 2) {
            return 0L;
        }
        long first = rows.get(0).totalMs();
        long best = rows.stream().mapToLong(Database.PlayerRunSample::totalMs).min().orElse(first);
        return Math.max(0L, first - best);
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
}

