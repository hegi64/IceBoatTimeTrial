package com.hegi64.iceBoatTimeTrial.service.settings;

public enum MessageVerbosity {
    DETAILED,
    NORMAL,
    MINIMAL;

    public static MessageVerbosity from(String raw) {
        if (raw == null || raw.isBlank()) {
            return NORMAL;
        }
        try {
            return MessageVerbosity.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return NORMAL;
        }
    }
}

