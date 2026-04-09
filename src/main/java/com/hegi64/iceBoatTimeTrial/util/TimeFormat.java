package com.hegi64.iceBoatTimeTrial.util;

public final class TimeFormat {
    private TimeFormat() {
    }

    public static String formatMillis(long millis) {
        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;
        long ms = millis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, ms);
    }

    public static String formatMillisOneDecimal(long millis) {
        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;
        long tenths = (millis % 1000) / 100;
        return String.format("%02d:%02d.%d", minutes, seconds, tenths);
    }

    public static String formatSignedMillis(long millis) {
        String sign = millis < 0 ? "-" : "+";
        return sign + formatMillis(Math.abs(millis));
    }

    public static String formatSignedMillisOneDecimal(long millis) {
        String sign = millis < 0 ? "-" : "+";
        return sign + formatMillisOneDecimal(Math.abs(millis));
    }
}
