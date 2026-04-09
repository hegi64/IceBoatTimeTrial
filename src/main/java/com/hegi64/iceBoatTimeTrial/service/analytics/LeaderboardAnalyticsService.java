package com.hegi64.iceBoatTimeTrial.service.analytics;

import java.util.List;
import java.util.UUID;

public interface LeaderboardAnalyticsService {
    List<LeaderboardEntry> getMostImproved(UUID trackUuid, PeriodFilter period, int limit);

    List<LeaderboardEntry> getMostConsistent(UUID trackUuid, PeriodFilter period, int limit, int sampleSize);

    List<TrackActivityEntry> getMostActiveTracks(PeriodFilter period, int limit);
}

