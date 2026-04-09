package com.hegi64.iceBoatTimeTrial.model;

public enum OverlapPolicy {
    FIRST_CREATED,
    NEAREST_CENTER;

    public static OverlapPolicy fromConfig(String raw) {
        if (raw == null) {
            return FIRST_CREATED;
        }
        try {
            return OverlapPolicy.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return FIRST_CREATED;
        }
    }
}

