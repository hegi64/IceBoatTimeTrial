package com.hegi64.iceBoatTimeTrial.service.settings;

public record PlayerSettings(boolean bossbarEnabled, MessageVerbosity verbosity) {
    public static PlayerSettings defaults() {
        return new PlayerSettings(true, MessageVerbosity.NORMAL);
    }
}
