package com.hegi64.iceBoatTimeTrial.service.analytics;

import java.util.UUID;

public record TrackActivityEntry(UUID trackUuid, String trackName, long runCount) {
}

