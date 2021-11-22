package com.vprokopiv.ytbot;

import com.google.api.client.util.DateTime;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodicJobTest {

    @Test
    void testRunTime() {
        long currentTs = System.currentTimeMillis();

        DateTime checkTime = PeriodicJob.getCheckTime(currentTs);
        var checkTs = checkTime.getValue();

        assertTrue(checkTs - currentTs < 50L);
    }
}
