package com.hegi64.iceBoatTimeTrial.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatTest {
    @Test
    void formatsMillisecondsAsExpected() {
        assertEquals("00:00.000", TimeFormat.formatMillis(0));
        assertEquals("00:01.234", TimeFormat.formatMillis(1234));
        assertEquals("01:01.001", TimeFormat.formatMillis(61001));
    }

    @Test
    void formatsSignedMillisecondsAsExpected() {
        assertEquals("+00:01.234", TimeFormat.formatSignedMillis(1234));
        assertEquals("-00:01.234", TimeFormat.formatSignedMillis(-1234));
    }

    @Test
    void formatsSignedOneDecimalAsExpected() {
        assertEquals("+00:01.2", TimeFormat.formatSignedMillisOneDecimal(1234));
        assertEquals("-00:01.2", TimeFormat.formatSignedMillisOneDecimal(-1234));
    }
}
