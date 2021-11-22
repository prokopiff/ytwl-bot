package com.vprokopiv.ytbot.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilTest {
    @Test
    void testFormatDuration1() {
        Duration duration = Duration.parse("PT1H19M6S");

        assertEquals("1:19:06", Util.formatDuration(duration));
    }

    @Test
    void testFormatDuration2() {
        Duration duration = Duration.parse("PT9M6S");

        assertEquals("09:06", Util.formatDuration(duration));
    }
}
