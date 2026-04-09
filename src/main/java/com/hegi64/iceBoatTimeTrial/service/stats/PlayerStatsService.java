package com.hegi64.iceBoatTimeTrial.service.stats;

import com.hegi64.iceBoatTimeTrial.service.analytics.PeriodFilter;

import java.util.Optional;
import java.util.UUID;

public interface PlayerStatsService {
    Optional<PlayerStatsSnapshot> getStats(UUID playerUuid, PeriodFilter period);
}

