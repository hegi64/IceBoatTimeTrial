package com.hegi64.iceBoatTimeTrial.service.analytics;

import java.util.UUID;

public record LeaderboardEntry(UUID playerUuid, String displayName, long value, String extra) {
}

