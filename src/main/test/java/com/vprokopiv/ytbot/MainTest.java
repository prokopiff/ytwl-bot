package com.vprokopiv.ytbot;

import com.google.api.client.util.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void testRunTime() {
        long currentTs = System.currentTimeMillis();

        DateTime checkTime = Main.getCheckTime(currentTs);
        var checkTs = checkTime.getValue();

        assertTrue(checkTs - currentTs < 50L);
    }

}
