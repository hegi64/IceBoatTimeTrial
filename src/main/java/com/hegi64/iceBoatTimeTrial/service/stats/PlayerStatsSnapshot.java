package com.hegi64.iceBoatTimeTrial.service.stats;

import java.util.UUID;

public record PlayerStatsSnapshot(
        UUID playerUuid,
        String playerName,
        long totalRuns,
        long totalCompletions,
        long bestOverallMs,
        long recentAverageMs,
        long improvementMs
) {
}

